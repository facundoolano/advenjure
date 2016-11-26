(ns advenjure.game
  #?(:cljs (:require-macros [cljs.core.async.macros :refer[go]][advenjure.async :refer [aloop alet let!?]]))
  (:require [advenjure.rooms :as room]
            #?(:clj [advenjure.async :refer [let!? aloop alet]])
            [advenjure.change-rooms :refer [change-rooms]]
            [advenjure.utils :as utils]
            [advenjure.verb-map :refer [find-verb default-map]]
            [advenjure.ui.input :refer [get-input exit read-key]]
            [advenjure.ui.output :refer [print-line init]]
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
    :events #{}
    :executed-dialogs #{}
    :points 0
    :moves 0
    :configuration {:start-message "" ; FIXME can be a function that takes game state
                    :end-message "\nThe End." ; FIXME can be a function that takes game state
                    :verb-map default-map
                    :prompt default-prompt
                    :hooks (zipmap hooks (repeat []))}}))

(defn use-plugin
  "Merges the given plugin spec into the given game configuration."
  [gs plugin-spec]
  (let [base-config (:configuration gs)
        merged-hooks (merge-with conj (:hooks base-config) (:hooks plugin-spec))
        merged-verbs (merge (:verb-map base-config) (:verb-map plugin-spec))
        new-config (merge base-config
                          plugin-spec
                          {:hooks merged-hooks}
                          {:verb-map merged-verbs})]
    (assoc gs :configuration new-config)))


(defn process-input
  "Take an input comand, find the verb in it and execute its action handler."
  [game-state input]
  (let [verb-map (get-in game-state [:configuration :verb-map])
        clean (clojure.string/trim (clojure.string/lower-case input))
        [verb tokens] (find-verb verb-map clean)
        handler (get verb-map verb)]
    (if handler
      (alet [new-state (update-in game-state [:moves] inc)
             handler-state (apply handler new-state tokens)]
        (or handler-state new-state))

      (do (if-not (clojure.string/blank? clean) (print-line (_ "I didn't know how to do that.")))
        game-state))))

(defn say-message
  [gs kw]
  (let [config (get-in gs [:configuration kw])
        value (if (fn? config) (config gs) config)]
    (utils/say value)))

(defn run
  "Run the game loop. Requires a finished? function to decide when to terminate
  the loop. The rest of the parameters are configuration key/values."
  [game-state finished? & {:as extras}]
  (let [game-state (use-plugin game-state extras)]
    (init)
    (say-message game-state :start-message)
    (alet [k (read-key)]
      (aloop [state (change-rooms game-state (:current-room game-state))]
        (let!? [input (get-input state)
                new-state (process-input state input)]
          (if-not (finished? new-state)
            (recur new-state)
            (do
             (say-message game-state :end-message)
             (exit))))))))
