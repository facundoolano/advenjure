(ns advenjure.ui.output
  (:require [jquery]
            [clojure.string :as string]))

(defn echo [text] (.echo (.terminal (js/$ "#terminal")) text))

(defn init []
  (.terminal (js/$ "#terminal")
             (fn [])
             (js-obj
              "prompt" "advenjure> "
              "greetings" false
              "clear" false
              "exit" false)))

(defn print-line
  [& strs]
  (let [joined (apply str (or strs [" "]))
        nonblank (if (string/blank? joined) " " joined)]
    (do
      (echo nonblank)
      nil))) ; need to return nil, echo doesnt


(defn write-file [file value]
  (aset js/localStorage file (pr-str value)))

