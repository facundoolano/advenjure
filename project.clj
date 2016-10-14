(defproject advenjure "0.4.0"
  :description "A text adveture engine"
  :url "https://github.com/facundoolano/advenjure"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [org.clojure/core.async "0.2.391"]
                 [gettext "0.1.1"]
                 [jline/jline "2.8"]]
  :plugins [[lein-cljsbuild "1.1.4"]]
  :cljsbuild
    {:builds
     [{:source-paths ["src"]
       :compiler {:output-dir "out"
                  :optimizations :none
                  :source-map true
                  :pretty-print true}}]}
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})




