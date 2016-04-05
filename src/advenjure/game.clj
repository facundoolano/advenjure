(ns advenjure.game
  (:require [advenjure.rooms :as room]
            [advenjure.utils :as utils]
            [advenjure.verb-map :refer [find-verb verb-map]]
            [advenjure.interface :refer [get-input]]))

(defn make
  "Make a new game state based on a room map and an optional initial inventory set."
  ([room-map start-room] (make room-map start-room #{}))
  ([room-map start-room inventory]
   {:room-map room-map
    :current-room start-room
    :inventory inventory
    :events #{}
    :executed-dialogs #{}}))

(defn process-input
  "Take an input comand, find the verb in it and execute its action handler."
  [game-state input]
  (let [clean (clojure.string/trim (clojure.string/lower-case input))
        [verb tokens] (find-verb clean)
        handler (get verb-map verb)]
    (if handler
      (or (apply handler game-state tokens) game-state)
      (do (utils/say "I don't know how to do that.") game-state))))

(defn run
  "Run the game loop. Requires a finished? function to decide when to terminate the loop."
  ([game-state finished?] (run game-state finished? ""))
  ([game-state finished? initial-msg]
   (utils/say initial-msg)
   (loop [state (utils/change-rooms game-state (:current-room game-state))]
     (let [input (get-input state verb-map)
           new-state (process-input state input)]
       (if-not (finished? new-state)
         (recur new-state))))
   (utils/say "\nThe End.")))
