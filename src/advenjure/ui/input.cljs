(ns advenjure.ui.input
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.reader :refer [read-string]]
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

(defn load-file [file]
  (read-string (aget js/localStorage file)))

(defn set-prompt [gs]
  (let [term (.terminal (.$ js/window "#terminal"))
        room (:name (current-room gs))
        moves (:moves gs)
        p (str "\n@" room " [" moves "] > ")]
    (.echo term " ")
    (.set_prompt term p)))

(defn get-input
  "Wait for input to be written in the input channel"
  [state verb-map]
  (go
    (set-prompt state)
    (<! input-chan)))
