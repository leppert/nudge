(defproject nudge "0.1.0"
  :description "A Clojure(Script) library for returning user friendly messages from spec explanations."
  :url "https://github.com/leppert/nudge"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure       "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.293"]
                 [com.cemerick/piggieback "0.2.1"]]
  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-npm       "0.6.2"]
            [lein-doo       "0.1.7"]]
  :npm {:dependencies [[source-map-support "0.4.6"]]}

  :doo {:build "test"
        :alias {:default [:node]}}

  :cljsbuild
  {:builds {:test {:source-paths ["src" "test"]
                   :compiler {:output-to     "target/nudge-test/nudge.js"
                              :output-dir    "target/nudge-test"
                              :target        :nodejs
                              :language-in   :ecmascript5
                              :optimizations :none
                              :main          nudge.test-runner}}}}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]})
