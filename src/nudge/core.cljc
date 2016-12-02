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

#?(:clj
   (defn- res [form]
     (cond
       (keyword? form) form
       (symbol? form) (c/or (-> form resolve ->sym) form)   
       (sequential? form) (walk/postwalk #(if (symbol? %) (res %) %) (unfn form))
       :else form))
   :cljs
   (defn- res [env form]
     (cond
       (keyword? form) form
       (symbol? form) (c/or (->> form (resolve env) ->sym) form)
       (sequential? form) (walk/postwalk #(if (symbol? %) (res env %) %) (unfn form))
       :else form)))

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
;; ORIGINAL

#?(:clj
   (defn- resolve-and-get-spec
     [k]
     (-> (if (symbol? k) (resolve k) k)
         get-spec)))

#?(:clj (defn problem->spec
          [e]
          (let [req-missing (empty? (:path e))]
            (c/or (if req-missing (resolve-and-get-spec 'contains?))
                  (get-spec (-> e :via last))
                  (resolve-and-get-spec (:pred e)))))
   :cljs (defn problem->spec
           [e]
           (let [req-missing (empty? (:path e))]
             (c/or (if req-missing (get-spec 'contains?))
                   (get-spec (-> e :via last)) ;; already resolved
                   (get-spec (:pred e))))))

(defn- problem->msg
  [e]
  (let [prop (c/or (get-in e [:path 0])
                   (-> e :pred last))
        spec (problem->spec e)]
    {prop [spec]}))

(defn messages
  [spec data]
  (->> (s/explain-data spec data)
       #?(:clj  :clojure.spec/problems
          :cljs :cljs.spec/problems)
       (map problem->msg)
       (apply merge)))
