# nudge

[![CircleCI](https://circleci.com/gh/leppert/nudge.svg?style=svg)](https://circleci.com/gh/leppert/nudge)

A Clojure(Script) library for converting `spec/explain` results into
error messages that might be useful to an end user.

Inspired by Ruby on Rails’ [`model.errors.messages`](http://guides.rubyonrails.org/active_record_validations.html#working-with-validation-errors-errors).

[![Clojars Project](https://img.shields.io/clojars/v/nudge.svg)](https://clojars.org/nudge)

```clojure
[nudge "0.1.0"]
```

## Usage

``` clojure
(ns foo
  (:require [clojure.spec :as s]
            [nudge.core :as n]
            [nudge.defaults]))

(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def ::email-type (s/and string? #(re-matches email-regex %)))

;; nudge.core/def defines the nudge message to be returned in the
;; event the data structure fails to pass the spec.
;; Nudge keeps a global registry of these messages, just like clojure.spec.
(n/def ::email-type "must be a valid email address")
(s/def ::email ::email-type)

(s/def ::person
  (s/keys :req [::name ::email]))

;; An invalid map
(n/messages ::person {::email "not-a-valid-email"})
;; => {::name "must be present"
;;     ::email "must be a valid email address"}

;; A valid map
(n/messages ::person {::name "John Smith" ::email "john@example.com"})
;; => nil
```

## Message Resolution

When a spec problem is encountered, Nudge does it best to look up
messages by resolving the spec based on available keywords. For
instance, in the example below because `::email` is an alias for
`::email-type`, it will return the same message even though one has
not been explicitly defined.

``` clojure
(ns foo
  (:require [nudge.core :as n]))

(s/def ::email-type string?)
(n/def ::email-type "must be a valid email address")
(s/def ::email ::email-type)

(n/messages ::email-type false) ; => "must be a valid email address"
(n/messages ::email false) ; => "must be a valid email address"
```

## Defaults

Default messages are defined in `nudge.defaults` and can be
initialized by requiring the namespace. ex:

``` clojure
(ns foo
  (:require [nudge.core :as n]
            [nudge.defaults]))
```

Both the Clojure and ClojureScript implementations provide two special
default symbols that can be overridden:

- `nudge.defaults/default`: returned when no message has been
specified for a failing predicate
- `nudge.defaults/key-missing`: returned when a map key has been
  specified as required in a map spec but that key is missing

When redefining these keys, use the fully qualified symbol, like
so:

``` clojure
(n/def nudge.defaults/default "did not meet a requirement")
```

## Clojure (JVM) Only Features

In Clojure, messages can be defined using not only keywords but also
symbols. This allows default messages to be defined using symbols that
resolve to predicates, like so:

``` clojure
(ns foo
  (:require [clojure.spec :as s]
            [nudge.core as n]))

(n/def string? "must be a string")
(s/def ::name string?)
(n/messages ::name 123) ; => "must be a string"
```

A handful of these are defined as defaults in `nudge.defaults`.

ClojureScript will gain this functionality
once [CLJ-2059](http://dev.clojure.org/jira/browse/CLJ-2059) is resolved.

## License

Copyright © 2016 Greg Leppert

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
