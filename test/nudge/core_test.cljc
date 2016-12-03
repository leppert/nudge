(ns nudge.core-test
  (:require [clojure.spec :as s]
            [nudge.core :as n]
            [nudge.defaults]
            #?@(:clj  [[clojure.test :refer :all]]
                :cljs [[cljs.test :refer-macros [deftest is testing]]])))

(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(def email-msg "must be a valid email address")
(s/def ::email-type (s/and string? #(re-matches email-regex %)))
(n/def ::email-type email-msg)
(s/def ::email ::email-type)

(s/def ::person
  (s/keys :req [::id ::name ::email ::created-at]))

(def person
  {::id 1 ::name "John Smith" ::email "john@example.com" ::created-at 12345})

(deftest core
  (testing "returns nil when the spec passes"
    (is (= nil (n/messages ::person person))))
  (testing "follows references to find valid message"
    (is (= email-msg (n/messages ::email "foo@bar")))))

(deftest non-maps
  (testing "returns a string message when the spec doesn't pass"
    (is (= email-msg (n/messages ::email-type "foo@bar")))))

(deftest maps
  (testing "returns a map of messages when the spec doesn't pass"
    (is (= {::id ["can't be blank"]} (n/messages ::person (dissoc person ::id))))))
