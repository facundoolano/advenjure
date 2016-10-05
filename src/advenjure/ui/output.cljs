(ns advenjure.ui.output
  (:require [jquery]))

(defn echo [text] (.echo (.terminal (js/$ "#terminal")) text))

(defn init []
  (.terminal (js/$ "#terminal")
             (fn [])
             (js-obj
              "prompt" "advenjure> "
              "completion" #(%3 (array "take", "open", "push")) ;FIXME just to test
              "greetings" false
              "clear" false
              "exit" false)))

(defn print-line
  [& strs]
  (do
    (echo (apply str (or strs [" "])))
    nil))


(defn save-file [file value]
  (aset js/localStorage file (pr-str value)))

