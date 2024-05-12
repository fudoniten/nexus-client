(ns nexus.client.cli
  (:require [nexus.client :as client]
            [fudo-clojure.ip :as ip]
            [fudo-clojure.logging :as log]
            [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [clojure.core.async :refer [chan >!! <!! go-loop timeout alt!]]
            [clojure.set :as set])
  (:import java.net.InetAddress
           [java.io StringWriter PrintWriter])
  (:gen-class))

(def cli-opts
  [["-4" "--ipv4" "Send IPv4 address to the DDNS server."
    :default true]
   ["-6" "--ipv6" "Send IPv6 address to the DDNS server."
    :default true]
   ["-f" "--sshfps SSHFPS_FILE" "Files containing SSHFPs for the current host."
    :default   []
    :multi     true
    :update-fn conj]
   ["-H" "--hostname HOSTNAME" "The name of this host."]
   ["-s" "--server SERVER" "Hostname(s) of the Nexus DDNS server."
    :multi     true
    :update-fn conj]
   ["-k" "--key-file FILE" "Location of host HMAC key file."]
   ["-p" "--port PORT" "Port on which the Nexus DDNS server is listening."
    :default 80
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a valid port number."]]
   ["-d" "--delay-seconds SECONDS" "Delay between reports."
    :default 360
    :parse-fn #(* 1000 (Integer/parseInt %))]
   ["-D" "--domain DOMAIN" "Domain to which this host belongs. May be specified more than once."
    :multi     true
    :update-fn conj]
   ["-c" "--certificate-authority CA" "Certificate authority trusted by the client."
    :multi     true
    :update-fn conj
    :default   []]
   ["-t" "--tailscale" "Report tailscale IPs instead of public IPs."
    :default false]
   ["-P" "--private" "Report priate IPs instead of public IPs."
    :default false]
   ["-h" "--help" "Print this mesage."]
   ["-v" "--verbose" "Verbose output."
    :default false]])

(defn- capture-stack-trace [e]
  (let [string-writer (StringWriter.)
        print-writer  (PrintWriter. string-writer)]
    (.printStackTrace e print-writer)
    (.flush print-writer)
    (.toString string-writer)))

(defn- parse-opts [args required cli-opts]
  (let [{:keys [options]
         :as result}     (cli/parse-opts args cli-opts)
        missing          (set/difference required (-> options keys set))
        missing-errors   (map #(format "missing required parameter: %s" %)
                              missing)]
    (update result :errors concat missing-errors)))

(defn- msg-quit [status msg]
  (println msg)
  (System/exit status))

(defn- usage
  ([summary] (usage summary []))
  ([summary errors] (->> (concat errors
                                 ["usage: nexus-client [opts]"
                                  ""
                                  "Options:"
                                  summary])
                         (str/join \newline))))

(defn- get-public-ipv4 []
  (->> (ip/get-host-ips)
       (filter ip/ipv4?)
       (filter ip/public?)
       (first)))

(defn- get-public-ipv6 []
  (->> (ip/get-host-ips)
       (filter ip/ipv6?)
       (filter ip/public?)
       (first)))

(defn- get-private-ipv4 []
  (->> (ip/get-host-ips)
       (filter ip/ipv4?)
       (filter ip/private?)
       (first)))

(defn- get-private-ipv6 []
  (->> (ip/get-host-ips)
       (filter ip/ipv6?)
       (filter ip/private?)
       (first)))

(defn- get-tailscale-ipv4 []
  (->> (ip/get-host-ips)
       (filter ip/ipv4?)
       (filter ip/tailscale?)
       (first)))

(defn- get-tailscale-ipv6 []
    (->> (ip/get-host-ips)
         (filter ip/ipv6?)
         (filter ip/tailscale?)
         (first)))

(defn- report-ipv4! [logger client ip verbose]
  (if ip
    (do
      (when verbose (println (str "reporting v4 ip: " ip)))
      (client/send-ipv4! client ip))
    (log/info! logger "no public ipv4 address found, skipping")))

(defn- report-ipv6! [logger client ip verbose]
  (if ip
    (do
      (when verbose (println (str "reporting v6 ip: " ip)))
      (client/send-ipv6! client ip))
    (log/info! logger "no public ipv6 address found, skipping")))

(defn- report-sshfps! [logger client sshfps verbose]
  (if (seq sshfps)
    (do
      (when verbose (println (str "reporting " (count sshfps) " ssh fingerprints")))
      (client/send-sshfps! client sshfps))
    (log/info! logger "no sshfps provided, skipping")))

(defprotocol DataFetcher
  (ipv4 [_])
  (ipv6 [_])
  (sshfps [_]))

(defn- public-fetcher [sshfps]
  (reify DataFetcher
    (ipv4 [_] (get-public-ipv4))
    (ipv6 [_] (get-public-ipv6))
    (sshfps [_] sshfps)))

(defn- private-fetcher [sshfps]
  (reify DataFetcher
    (ipv4 [_] (get-private-ipv4))
    (ipv6 [_] (get-private-ipv6))
    (sshfps [_] sshfps)))

(defn- tailscale-fetcher [sshfps]
  (reify DataFetcher
    (ipv4 [_] (get-tailscale-ipv4))
    (ipv6 [_] (get-tailscale-ipv6))
    (sshfps [_] sshfps)))

(defn- execute! [& {:keys [timeout-ms logger client sshfps verbose fetcher]}]
  (report-sshfps! logger client (sshfps fetcher) verbose)
  (let [stop-chan (chan)]
    (go-loop [continue true]
      (if continue
        (do (try
              (report-ipv4! logger client (ipv4 fetcher) verbose)
              (report-ipv6! logger client (ipv6 fetcher) verbose)
              (catch Exception e
                (println (capture-stack-trace e))))
            (recur (alt! (timeout timeout-ms) true
                         stop-chan            false)))
        nil))))

(defn -main [& args]
  (let [{:keys [options _ errors summary]} (parse-opts args #{:server :key-file} cli-opts)]
    (when (seq errors)    (msg-quit 1 (usage summary errors)))
    (when (:help options) (msg-quit 0 (usage summary)))
    (when (empty? (:server options))
      (msg-quit 1 (usage summary ["At least one server must be specified."])))
    (let [hostname       (or (:hostname options)
                             (-> (InetAddress/getLocalHost) (.getHostName)))
          client         (client/combine-nexus-clients
                          (map (fn [domain]
                                 (println (str "initializing domain: ${domain}"))
                                 (client/connect :verbose  (:verbose options)
                                                 :domain   domain
                                                 :hostname hostname
                                                 :servers  (:server options)
                                                 :port     (:port options)
                                                 :hmac-key (-> options :key-file slurp)
                                                 :logger   (log/print-logger)
                                                 :ca-map   (into {}
                                                                 (map-indexed
                                                                  (fn [i cert]
                                                                    [(keyword (str "ca" i))
                                                                     cert])
                                                                  (:certificate-authority options)))))
                               (:domain options)))
          logger         (log/print-logger)
          sshfps         (some->> (:sshfps options)
                                  (map slurp)
                                  (mapcat str/split-lines))
          data-fetcher   (cond (:tailscale options) (tailscale-fetcher sshfps)
                               (:public options)    (private-fetcher   sshfps)
                               :else                (public-fetcher sshfps))
          stop-chan      (execute! :timeout-ms (:delay-seconds options)
                                   :logger     logger
                                   :client     client
                                   :fetcher    data-fetcher
                                   :verbose    (:verbose options))
          catch-shutdown (chan)]
      (.addShutdownHook (Runtime/getRuntime)
                        (Thread. (fn [] (>!! catch-shutdown true))))
      (<!! catch-shutdown)
      (>!! stop-chan true)
      (System/exit 0))))
