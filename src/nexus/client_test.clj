(ns nexus.client-test)
(ns nexus.client-test
  (:require [clojure.test :refer :all]
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

(deftest test-put-host-record-request
  (testing "put-host-record-request function"
    (let [req (put-host-record-request :ipv4 {:hostname "test" :domain "example.com" :server "localhost" :port 8080 :value "127.0.0.1"})]
      (is (= "PUT" (req/method req)))
      (is (= "/api/v2/domain/example.com/host/test/ipv4" (req/request-path req)))
      (is (= "127.0.0.1" (req/body req))))))

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
