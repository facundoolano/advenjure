(ns advenjure.ui.output
  (:require [jquery]
            [clojure.string :as string]
            [advenjure.ui.string-wrap :refer [string-wrap]]))

(defn echo [text] (.echo (.terminal (js/$ "#terminal")) text))

(defn clear []
  (.clear (.terminal (js/$ "#terminal"))))

(defn init []
  (.terminal (js/$ "#terminal")
             (fn [])
             (js-obj
              "prompt" "advenjure> "
              "greetings" false
              "scrollOnEcho" true
              "clear" false
              "exit" false)))

(defn print-line
  [& strs]
  (let [joined (apply str (or strs [" "]))
        nonblank (if (string/blank? joined) " \n" joined)]
    (echo (string-wrap nonblank))
    nil)) ; need to return nil, echo doesnt

(defn write-file [file value]
  (aset js/localStorage file (pr-str value)))
