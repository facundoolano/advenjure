(ns advenjure.ui.input
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.reader :refer [read-string]]
            [cljs.reader :refer [read-string]]
            [advenjure.ui.input-channel :refer [input-chan]]
            [advenjure.utils :refer [current-room]]
            [cljs.core.async :refer [<! >! chan]]))

; FIXME on read value set a once listener, get value, unset
(defn read-value []
  (let [key-chan (chan)]
    (go
      (.pause (.terminal (.$ js/window "#terminal")))
      (aset js/window "onkeydown"
        (fn [ev]
          (do
            (aset js/window "onkeydown" nil)
            (println (aget ev "key"))
            (go (>! key-chan (aget ev "key")))
            (.resume (.terminal (.$ js/window "#terminal"))))))
      (read-string (<! key-chan)))))

; TODO
(def read-key read-value)

(defn slurp [file])

(defn set-prompt [gs]
  (let [room (:name (current-room gs))
        moves (:moves gs)
        p (str "\n@" room " [" moves "] > ")]
    (.set_prompt (.terminal (.$ js/window "#terminal")) p)))

(defn get-input
  "Wait for input to be written in the input channel"
  [state verb-map]
  (go
    (set-prompt state)
    (<! input-chan)))
