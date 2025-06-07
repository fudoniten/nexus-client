(ns nexus.client-test
  (:require [clojure.test :refer :all]
            [fudo-clojure.http.request :as req]
            [nexus.client :refer :all]))

(deftest test-build-path
  (testing "build-path function"
    (is (= "/api/v2/domain/example.com/host/test/ipv4"
           (build-path :api :v2 :domain "example.com" :host "test" :ipv4)))))

(deftest test-base-request
  (testing "base-request function"
    (let [req (base-request "localhost" 8080)]
      (is (= "localhost" (req/host req)))
      (is (= 8080 (req/port req))))))

(deftest test-send-ipv4-request
  (testing "send-ipv4-request function"
    (let [req (send-ipv4-request :hostname "test" :domain "example.com" :server "localhost" :port 8080 :ip "127.0.0.1")]
      (is (= "PUT" (req/method req)))
      (is (= "/api/v2/domain/example.com/host/test/ipv4" (req/request-path req)))
      (is (= "127.0.0.1" (req/body req))))))

(deftest test-send-ipv6-request
  (testing "send-ipv6-request function"
    (let [req (send-ipv6-request :hostname "test" :domain "example.com" :server "localhost" :port 8080 :ip "::1")]
      (is (= "PUT" (req/method req)))
      (is (= "/api/v2/domain/example.com/host/test/ipv6?" (req/request-path req)))
      (is (= "::1" (req/body req))))))

(deftest test-send-sshfps-request
  (testing "send-sshfps-request function"
    (let [req (send-sshfps-request :hostname "test" :domain "example.com" :server "localhost" :port 8080 :sshfps "sshfp-data")]
      (is (= "PUT" (req/method req)))
      (is (= "/api/v2/domain/example.com/host/test/sshfps" (req/request-path req)))
      (is (= "sshfp-data" (req/body req))))))

(deftest test-make-signature-generator
  (testing "make-signature-generator function"
    (let [sign (make-signature-generator "secret-key")]
      (is (string? (sign "message"))))))

(deftest test-make-request-authenticator
  (testing "make-request-authenticator function"
    (let [authenticator (make-request-authenticator {::hmac-key "secret-key" ::hostname "test-host"})
          req (base-request "localhost" 8080)
          authenticated-req (authenticator req)]
      (is (contains? (req/headers authenticated-req) :access-signature))
      (is (contains? (req/headers authenticated-req) :access-timestamp))
      (is (contains? (req/headers authenticated-req) :access-hostname)))))
(ns nexus.client-test
  (:require [clojure.test :refer :all]
            [nexus.client :refer :all]))

(deftest test-to-path-elem
  (testing "to-path-elem function"
    (is (= "keyword" (to-path-elem :keyword)))
    (is (= "string" (to-path-elem "string")))
    (is (thrown? Exception (to-path-elem 123)))))

(deftest test-build-path
  (testing "build-path function"
    (is (= "/api/v2/domain/example.com/host/test/ipv4"
           (build-path :api :v2 :domain "example.com" :host "test" :ipv4)))))

(deftest test-base-request
  (testing "base-request function"
    (let [req (base-request "localhost" 8080)]
      (is (= "localhost" (req/host req)))
      (is (= 8080 (req/port req))))))

(deftest test-send-ipv4-request
  (testing "send-ipv4-request function"
    (let [req (send-ipv4-request :hostname "test" :domain "example.com" :server "localhost" :port 8080 :ip "127.0.0.1")]
      (is (= "PUT" (req/method req)))
      (is (= "/api/v2/domain/example.com/host/test/ipv4" (req/request-path req)))
      (is (= "127.0.0.1" (req/body req))))))

(deftest test-send-ipv6-request
  (testing "send-ipv6-request function"
    (let [req (send-ipv6-request :hostname "test" :domain "example.com" :server "localhost" :port 8080 :ip "::1")]
      (is (= "PUT" (req/method req)))
      (is (= "/api/v2/domain/example.com/host/test/ipv6" (req/request-path req)))
      (is (= "::1" (req/body req))))))

(deftest test-send-sshfps-request
  (testing "send-sshfps-request function"
    (let [req (send-sshfps-request :hostname "test" :domain "example.com" :server "localhost" :port 8080 :sshfps "sshfp-data")]
      (is (= "PUT" (req/method req)))
      (is (= "/api/v2/domain/example.com/host/test/sshfps" (req/request-path req)))
      (is (= "sshfp-data" (req/body req))))))

(deftest test-make-signature-generator
  (testing "make-signature-generator function"
    (let [sign (make-signature-generator "secret-key")]
      (is (string? (sign "message"))))))

(deftest test-make-request-authenticator
  (testing "make-request-authenticator function"
    (let [authenticator (make-request-authenticator {::hmac-key "secret-key" ::hostname "test-host"})
          req (base-request "localhost" 8080)
          authenticated-req (authenticator req)]
      (is (contains? (req/headers authenticated-req) :access-signature))
      (is (contains? (req/headers authenticated-req) :access-timestamp))
      (is (contains? (req/headers authenticated-req) :access-hostname)))))
(ns nexus.client.cli-test
  (:require [clojure.test :refer :all]
            [nexus.client.cli :refer :all]))

(deftest test-capture-stack-trace
  (testing "capture-stack-trace function"
    (let [e (Exception. "Test exception")]
      (is (string? (capture-stack-trace e))))))

(deftest test-parse-opts
  (testing "parse-opts function"
    (let [args ["--hostname" "test-host" "--server" "localhost"]
          required #{:server :hostname}
          result (parse-opts args required cli-opts)]
      (is (empty? (:errors result)))
      (is (= "test-host" (:hostname (:options result)))))))

(deftest test-msg-quit
  (testing "msg-quit function"
    (is (thrown? System$Exit (msg-quit 0 "Exiting")))))

(deftest test-usage
  (testing "usage function"
    (let [summary "Summary of options"]
      (is (string? (usage summary)))
      (is (string? (usage summary ["Error message"]))))))

(deftest test-get-public-ipv4
  (testing "get-public-ipv4 function"
    (is (nil? (get-public-ipv4)))))

(deftest test-get-public-ipv6
  (testing "get-public-ipv6 function"
    (is (nil? (get-public-ipv6)))))

(deftest test-get-private-ipv4
  (testing "get-private-ipv4 function"
    (is (nil? (get-private-ipv4)))))

(deftest test-get-private-ipv6
  (testing "get-private-ipv6 function"
    (is (nil? (get-private-ipv6)))))

(deftest test-get-tailscale-ipv4
  (testing "get-tailscale-ipv4 function"
    (is (nil? (get-tailscale-ipv4)))))

(deftest test-get-tailscale-ipv6
  (testing "get-tailscale-ipv6 function"
    (is (nil? (get-tailscale-ipv6)))))
(ns nexus.client-test
  (:require [clojure.test :refer :all]
            [nexus.client :refer :all]))

(deftest test-to-path-elem
  (testing "to-path-elem function"
    (is (= "keyword" (to-path-elem :keyword)))
    (is (= "string" (to-path-elem "string")))
    (is (thrown? Exception (to-path-elem 123)))))

(deftest test-build-path
  (testing "build-path function"
    (is (= "/api/v2/domain/example.com/host/test/ipv4"
           (build-path :api :v2 :domain "example.com" :host "test" :ipv4)))))

(deftest test-base-request
  (testing "base-request function"
    (let [req (base-request "localhost" 8080)]
      (is (= "localhost" (req/host req)))
      (is (= 8080 (req/port req))))))

(deftest test-send-ipv4-request
  (testing "send-ipv4-request function"
    (let [req (send-ipv4-request :hostname "test" :domain "example.com" :server "localhost" :port 8080 :ip "127.0.0.1")]
      (is (= "PUT" (req/method req)))
      (is (= "/api/v2/domain/example.com/host/test/ipv4" (req/request-path req)))
      (is (= "127.0.0.1" (req/body req))))))

(deftest test-send-ipv6-request
  (testing "send-ipv6-request function"
    (let [req (send-ipv6-request :hostname "test" :domain "example.com" :server "localhost" :port 8080 :ip "::1")]
      (is (= "PUT" (req/method req)))
      (is (= "/api/v2/domain/example.com/host/test/ipv6" (req/request-path req)))
      (is (= "::1" (req/body req))))))

(deftest test-send-sshfps-request
  (testing "send-sshfps-request function"
    (let [req (send-sshfps-request :hostname "test" :domain "example.com" :server "localhost" :port 8080 :sshfps "sshfp-data")]
      (is (= "PUT" (req/method req)))
      (is (= "/api/v2/domain/example.com/host/test/sshfps" (req/request-path req)))
      (is (= "sshfp-data" (req/body req))))))

(deftest test-make-signature-generator
  (testing "make-signature-generator function"
    (let [sign (make-signature-generator "secret-key")]
      (is (string? (sign "message"))))))

(deftest test-make-request-authenticator
  (testing "make-request-authenticator function"
    (let [authenticator (make-request-authenticator {::hmac-key "secret-key" ::hostname "test-host"})
          req (base-request "localhost" 8080)
          authenticated-req (authenticator req)]
      (is (contains? (req/headers authenticated-req) :access-signature))
      (is (contains? (req/headers authenticated-req) :access-timestamp))
      (is (contains? (req/headers authenticated-req) :access-hostname)))))
(ns nexus.client.cli-test
  (:require [clojure.test :refer :all]
            [nexus.client.cli :refer :all]))

(deftest test-capture-stack-trace
  (testing "capture-stack-trace function"
    (let [e (Exception. "Test exception")]
      (is (string? (capture-stack-trace e))))))

(deftest test-parse-opts
  (testing "parse-opts function"
    (let [args ["--hostname" "test-host" "--server" "localhost"]
          required #{:server :hostname}
          result (parse-opts args required cli-opts)]
      (is (empty? (:errors result)))
      (is (= "test-host" (:hostname (:options result)))))))

(deftest test-msg-quit
  (testing "msg-quit function"
    (is (thrown? System$Exit (msg-quit 0 "Exiting")))))

(deftest test-usage
  (testing "usage function"
    (let [summary "Summary of options"]
      (is (string? (usage summary)))
      (is (string? (usage summary ["Error message"]))))))

(deftest test-get-public-ipv4
  (testing "get-public-ipv4 function"
    (is (nil? (get-public-ipv4)))))

(deftest test-get-public-ipv6
  (testing "get-public-ipv6 function"
    (is (nil? (get-public-ipv6)))))

(deftest test-get-private-ipv4
  (testing "get-private-ipv4 function"
    (is (nil? (get-private-ipv4)))))

(deftest test-get-private-ipv6
  (testing "get-private-ipv6 function"
    (is (nil? (get-private-ipv6)))))

(deftest test-get-tailscale-ipv4
  (testing "get-tailscale-ipv4 function"
    (is (nil? (get-tailscale-ipv4)))))

(deftest test-get-tailscale-ipv6
  (testing "get-tailscale-ipv6 function"
    (is (nil? (get-tailscale-ipv6)))))
