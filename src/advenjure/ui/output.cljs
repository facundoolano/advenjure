(ns advenjure.ui.output)

(defn echo [text] (.echo (.terminal (.$ js/window "#terminal")) text))

(defn init []
  (.terminal (.$ js/window "#terminal")
             (fn [])
             (js-obj
              "prompt" "advenjure> "
              "completion" #(%3 (array "take", "open", "push")) ;FIXME just to test
              "greetings" false
              "outputLimit" 0)))

(defn print-line
  [& strs]
  (do
    (echo (apply str (or strs [" "])))
    nil))


(defn save-file [file value]
  (aset js/localStorage file (pr-str value)))

