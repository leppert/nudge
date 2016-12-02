(ns nudge.defaults
  (:require [nudge.core :as n]))

(n/def clojure.core/contains? "can't be blank")
(n/def clojure.core/string? "must be a string")
(n/def clojure.core/keyword? "must be a keyword")
(n/def clojure.core/number? "must be a number")
(n/def clojure.core/int? "must be a fixed precision integer")
(n/def clojure.core/integer? "must be an integer")
(n/def clojure.core/even? "must be an even number")
(n/def clojure.core/odd? "must be an odd number")
(n/def clojure.core/boolean? "must be a boolean")
