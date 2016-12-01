# nudge

[![CircleCI](https://circleci.com/gh/leppert/nudge.svg?style=svg)](https://circleci.com/gh/leppert/nudge)

A Clojure(Script) library for converting `spec/explain` results into
error messages that might be useful to an end user.

Inspired by Ruby on Rails’ [`model.errors.messages`](http://guides.rubyonrails.org/active_record_validations.html#working-with-validation-errors-errors).

## Usage

``` clojure
(ns my-namespace
  (:require [nudge.core as n]
            [nudge.defaults]))

(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def ::email-type (s/and string? #(re-matches email-regex %)))

;; nudge.core/def defines the nudge message to be returned in the
;; event the data structure fails to pass the spec.
;; Nudge keeps a global registry of these messages, just like specs.
(n/def ::email-type "must be a valid email address")
(s/def ::email ::email-type)

;; the message for string? is defined in nudge.defaults
(s/def ::name string?)

(n/messages ::person {::name 1 ::email "not-a-valid-email"})
;; => {::name [“must be a string”]
;;     ::email [“must be a valid email address”]}

(n/messages ::person {::name “John Smith” ::email "john@example.com"})
;; => nil
```

## License

Copyright © 2016 Greg Leppert

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
