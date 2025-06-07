(ns nexus.client.cli-test
  (:require [nexus.client.cli :as sut]
            [clojure.test :as t]))
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
