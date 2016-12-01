(ns nudge.defaults
  (:require [nudge.core :as n]))

(n/def string? "must be a string")
(n/def keyword? "must be a keyword")
(n/def number? "must be a number")
(n/def int? "must be a fixed precision integer")
(n/def integer? "must be an integer")
(n/def even? "must be an even number")
(n/def odd? "must be an odd number")
(n/def boolean? "must be a boolean")
