(defproject advenjure "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [jline/jline "2.8"]]
  :main ^:skip-aot advenjure.example.game
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

