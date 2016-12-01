(ns nudge.core-test
  (:require [nudge.core :as n]
            [nudge.defaults]
            [clojure.spec :as s]
            #?@(:clj  [[clojure.test :refer :all]]
                :cljs [[cljs.test :refer-macros [deftest is testing]]])))

(s/def ::id integer?)
(s/def ::name string?)

(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def ::email-type (s/and string? #(re-matches email-regex %)))
(n/def ::email-type "must be a valid email address")
(s/def ::email ::email-type)

(s/def ::person
  (s/keys :req [::id ::name ::email ::created-at]))

(deftest defaults
  (is (= {::created-at ["can't be blank"]} (n/messages ::person {::id 1 ::name "John Smith" ::email "john@example.com"}))))
