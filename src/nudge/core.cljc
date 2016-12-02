(ns nudge.core
  (:refer-clojure :exclude [+ * and or def #?(:cljs resolve) assert])
  (:require [clojure.core :as c]
            [clojure.walk :as walk]
            [clojure.spec :as s]
            #?@(:cljs [[cljs.analyzer :as ana]
                       [cljs.env :as env]
                       [cljs.analyzer.api :refer [resolve]]]))
  #?(:cljs (:require-macros nudge.core)))

;; ---------------------------
;; FROM clojure.spec
;; https://github.com/clojure/clojure/blob/d920ada9fab7e9b8342d28d8295a600a814c1d8a/src/clj/clojure/spec.clj
;; AND cljs.spec
;; https://github.com/clojure/clojurescript/blob/de05b15568c848a1d4f80a44bdffd486abd05150/src/main/cljs/cljs/spec.cljc
;; https://github.com/clojure/clojurescript/blob/de05b15568c848a1d4f80a44bdffd486abd05150/src/main/cljs/cljs/spec.cljs

(defonce ^:private registry-ref (atom {}))

#?(:clj (defn- named? [x] (instance? clojure.lang.Named x)))

(defn- ->sym
  "Returns a symbol from a symbol or var"
  [x]
  #?(:clj (if (var? x)
            (let [^clojure.lang.Var v x]
              (symbol (str (.name (.ns v)))
                      (str (.sym v))))
            x)
     :cljs (if (var? x)
             (.-sym x)
             x)))

(defn- unfn [expr]
  (if (c/and (seq? expr)
             (symbol? (first expr))
             (= "fn*" (name (first expr))))
    (let [[[s] & form] (rest expr)]
      (conj (walk/postwalk-replace {s '%} form) '[%] 'fn))
    expr))

(defn- res [#?(:cljs env) form]
  (cond
    (keyword? form) form
    (symbol? form) (c/or (->> form (resolve #?(:cljs env)) ->sym) form)
    (sequential? form) (walk/postwalk #(if (symbol? %) (res #?(:cljs env) %) %) (unfn form))
    :else form))

(defn ^:skip-wiki def-impl
  "Do not call this directly, use 'def'"
  [k form spec]
  (c/assert (c/and #?(:clj  (named? k)
                      :cljs (ident? k)) (namespace k)) "k must be namespaced keyword or resolvable symbol")
  (swap! registry-ref assoc k spec)
  k)

#?(:clj
   (defn- ns-qualify
     "Qualify symbol s by resolving it or using the current *ns*."
     [s]
     (if-let [ns-sym (some-> s namespace symbol)]
       (c/or (some-> (get (ns-aliases *ns*) ns-sym) str (symbol (name s)))
             s)
       (symbol (str (.name *ns*)) (str s))))
   :cljs
   (defn- ns-qualify
     "Qualify symbol s by resolving it or using the current *ns*."
     [env s]
     (if (namespace s)
       (let [v (resolve env s)]
         (c/assert v (str "Unable to resolve: " s))
         (->sym v))
       (symbol (str ana/*cljs-ns*) (str s)))))

(defmacro def
  "Given a namespace-qualified keyword or resolvable symbol k, and a
  spec, spec-name, predicate or regex-op makes an entry in the
  registry mapping k to the spec"
  [k spec-form]
  (let [k (if (symbol? k) (ns-qualify #?(:cljs &env) k) k)
        form (res #?(:cljs &env) spec-form)]
    `(def-impl '~k '~form ~spec-form)))

(defn registry
  "returns the registry map, prefer 'get-spec' to lookup a spec by name"
  []
  @registry-ref)

(defn get-spec
  "Returns spec registered for keyword/symbol/var k, or nil."
  [k]
  (get (registry) (if (keyword? k) k (->sym k))))

;; ---------------------------
;; NUDGE CORE

#?(:clj (defn- resolve-and-get-spec
          [k]
          (-> (if (symbol? k) (resolve k) k)
              get-spec)))

(defn- problem->spec
  [prob]
  (let [req-missing (c/and (map? (:val prob))
                           (empty? (:path prob)))]
    (c/or (if req-missing (get-spec 'nudge.defaults/key-missing))
          (get-spec (-> prob :via last))
          #?(:clj  (resolve-and-get-spec (:pred prob))
             :cljs (get-spec (:pred prob)))
          (get-spec 'nudge.defaults/default))))

(defn- problem->msg
  [prob]
  (let [prop (c/or (get-in prob [:path 0])
                   (-> prob :pred last))
        spec (problem->spec prob)]
    {prop [spec]}))

(defn messages
  [spec data]
  (if-let [probs (-> (s/explain-data spec data)
                     #?(:clj  :clojure.spec/problems
                        :cljs :cljs.spec/problems))]
    (if (seq? probs)
      (->> probs
           (map problem->msg)
           (apply merge))
      (problem->spec (probs 0)))))
