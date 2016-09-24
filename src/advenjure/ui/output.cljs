(ns advenjure.ui.output
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [>!]]
            [advenjure.ui.input-channel :refer [input-chan]]))

(defn echo [text] (.echo (.terminal (.$ js/window "#terminal")) text))

(defn process-command
  "Write command to the input channel"
  [command term]
  (go (>! input-chan command)))

(defn init []
  (.terminal (.$ js/window "#terminal")
             process-command
             (js-obj
              "prompt" "advenjure> "
              "greetings" false
              "outputLimit" 0)))

(defn print-line
  [& strs]
  (do
    (echo (apply str (or strs [" "])))
    nil))


(defn save-file [file value]
  (aset js/localStorage file (pr-str value)))

