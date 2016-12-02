(ns nudge.defaults-test
  (:require [nudge.core :as n]
            [nudge.defaults]
            [clojure.spec :as s]
            [clojure.test :refer :all]))

(s/def ::id integer?)
(s/def ::name string?)

(s/def ::person
  (s/keys :req [::id ::name]))

(def person
  {::id 1 ::name "John Smith"})

(deftest defaults
  (testing "required / some?"
    (is (= {::id ["can't be blank"]} (n/messages ::person (dissoc person ::id)))))
  (testing "string?"
    (is (= {::name ["must be a string"]} (n/messages ::person (assoc person ::name nil))))))
