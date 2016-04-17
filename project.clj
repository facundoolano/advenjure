(defproject advenjure "0.1.0-SNAPSHOT"
  :description "A text adveture engine"
  :url "https://github.com/facundoolano/advenjure"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [jline/jline "2.8"]]
  :main ^:skip-aot advenjure.example.game
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :gettext-source advenjure.text.en-past/dictionary)

