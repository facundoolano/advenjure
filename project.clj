(defproject advenjure "1.0.0-SNAPSHOT"
  :description "A text adveture engine"
  :url "https://github.com/facundoolano/advenjure"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [org.clojure/core.async "0.2.391"]
                 [gettext "0.1.1"]
                 [sonian/carica "1.2.2"]
                 [jline/jline "2.8"]
                 [cljs-ajax "0.5.8"]
                 [org.clojure/data.json "0.2.6"]
                 [eftest "0.3.0"]]
  :test-selectors {:default (complement :skip)
                   :focus   :focus}
  :plugins [[lein-cljsbuild "1.1.4"]]
  :cljsbuild
  {:builds
   [{:source-paths ["src"]
     :compiler     {:output-dir    "out"
                    :optimizations :none
                    :source-map    true
                    :pretty-print  true}}]}
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
