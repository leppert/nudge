(ns nudge.defaults-test
  (:require [nudge.core :as n]
            [nudge.defaults]
            [clojure.spec :as s]
            #?@(:clj  [[clojure.test :refer :all]]
                :cljs [[cljs.test :refer-macros [deftest is testing]]])))

(defn undocumented-pred [val] (= 1 val))
(s/def ::id undocumented-pred)
(s/def ::name string?)

(s/def ::person
  (s/keys :req [::id ::name]))

(def person
  {::id 1 ::name "John Smith"})

(deftest defaults
  (testing "default"
    (is (= {::id "did not meet a requirement"}
           (n/messages ::person (assoc person ::id 2)))))
  (testing "key-missing"
    (is (= {::name "can't be blank"}
           (n/messages ::person (dissoc person ::name))))))

#?(:clj
   (deftest clj-defaults
     (testing "string?"
       (is (= {::name "must be a string"}
              (n/messages ::person (assoc person ::name nil)))))))
