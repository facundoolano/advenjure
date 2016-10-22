(ns advenjure.ui.output
  (:require [jquery]
            [clojure.string :as string]))

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

(defn print
  [& strs]
  (let [joined (apply str (or strs [" "]))
        previous (or (aget js/window "_term_buffer") "")]
    (aset js/window "_term_buffer" (str previous joined)))) ;ugly hack, dont tell on me


(defn print-line
  [& strs]
  (let [joined (apply str (or strs [" "]))
        nonblank (if (string/blank? joined) " " joined)
        previous (or (aget js/window "_term_buffer") "")]
    (do
      (aset js/window "_term_buffer" "")
      (echo (str previous nonblank))
      nil))) ; need to return nil, echo doesnt


(defn write-file [file value]
  (aset js/localStorage file (pr-str value)))

