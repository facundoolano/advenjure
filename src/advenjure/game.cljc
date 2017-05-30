(ns advenjure.game
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [clojure.string :as string]
            #?(:clj [clojure.core.async :refer [go <!]]
               :cljs [cljs.core.async :refer [<!]])
            #?(:clj [clojure.core.async.impl.protocols :refer [ReadPort]]
               :cljs [cljs.core.async.impl.protocols :refer [ReadPort]])
            [advenjure.change-rooms :refer [change-rooms]]
            [advenjure.utils :as utils]
            [advenjure.hooks :as hooks]
            [advenjure.verb-map :refer [find-verb default-map]]
            [advenjure.ui.input :refer [get-input exit read-key]]
            [advenjure.ui.output :refer [print-line init clear]]
            [advenjure.gettext.core :refer [_]]))

(defn default-prompt
  [gs]
  (str "\n@" (:name (utils/current-room gs)) " [" (:moves gs) "] > "))

(def hooks #{:before-change-room
             :after-change-room
             :before-handler
             :after-handler
             :before-item-handler
             :after-item-handler})

(defn make
  "Make a new game state based on a room map and an optional initial inventory set."
  ([room-map start-room] (make room-map start-room #{}))
  ([room-map start-room inventory]
   {:room-map room-map
    :current-room start-room
    :inventory inventory
    :out ""
    :events #{}
    :executed-dialogs #{}
    :moves 0
    :configuration {:start-message ""
                    :end-message "\nThe End."
                    :verb-map default-map
                    :prompt default-prompt
                    :hooks (zipmap hooks (repeat []))}}))

(defn use-plugin
  "Merges the given plugin spec into the given game configuration."
  [gs plugin-spec]
  (let [base-config  (:configuration gs)
        merged-hooks (merge-with conj (:hooks base-config) (:hooks plugin-spec))
        merged-verbs (merge (:verb-map base-config) (:verb-map plugin-spec))
        new-config   (merge base-config
                            plugin-spec
                            {:hooks merged-hooks}
                            {:verb-map merged-verbs})]
    (assoc gs :configuration new-config)))

(defn force-channel [value]
  (go
    (if (satisfies? ReadPort value) (<! value) value)))

;; FIXME coerce handler to channel
(defn process-input
  "Take an input comand, find the verb in it and execute its action handler."
  [game-state input]
  (let [verb-map      (get-in game-state [:configuration :verb-map])
        clean         (string/trim (string/lower-case input))
        [verb tokens] (find-verb verb-map clean)
        handler       (get verb-map verb)]
    (go
      (if handler
        (let [before-state  (-> game-state
                                (update-in [:moves] inc)
                                (hooks/execute :before-handler))
              handler-state (-> (apply handler before-state tokens) force-channel <!)
              after-state   (hooks/execute (or handler-state before-state) :after-handler)]
          after-state)
        (if-not (string/blank? clean)
          (utils/say game-state (_ "I didn't know how to do that."))
          game-state)))))

(defn print-message
  [gs kw]
  (let [config (get-in gs [:configuration kw])
        value (if (fn? config) (config gs) config)]
    (when-not (string/blank? value)
      (print-line (utils/capfirst value))
      (read-key))))

(defn flush-output [gs]
  (doseq [output (string/split-lines (:out gs))]
    (if (utils/clear? output)
      (clear)
      (print-line (utils/capfirst output))))
  (assoc gs :out ""))

(defn run
  "Run the game loop. Requires a finished? function to decide when to terminate
  the loop. The rest of the parameters are configuration key/values."
  [game-state finished? & {:as extras}]
  (let [game-state (use-plugin game-state (merge extras {:finished finished?}))]
    (init)
    (go
      (<! (print-message game-state :start-message))
      (let [intial-state (change-rooms game-state (:current-room game-state))]
        (loop [state (flush-output intial-state)]
          (let [input     (<! (get-input state))
                new-state (<! (process-input state input))
                new-state (flush-output new-state)]
            (if-not ((get-in new-state [:configuration :finished]) new-state)
              (recur new-state)
              (do
                (print-message game-state :end-message)
                (exit)))))))))
