(ns advenjure.game
  (:require [advenjure.rooms :as room]
            [advenjure.verbs :as verb])) ;;FIXME shouldn't require much of this stuff


(defn make
  "Make a new game state based on a room map and an optional initial inventory set."
  ([room-map start-room] (make room-map start-room #{}))
  ([room-map start-room inventory]
   {:room-map room-map :current-room start-room :inventory inventory}))

(defn get-input
  ([] (get-input "\n>"))
  ([prefix]
   (do (print prefix) (flush) (read-line))))

(defn run
  "Run the game loop. Requires a finished? function to decide when to terminate the loop."
  ([game-state finished?] (run game-state finished? ""))
  ([game-state finished? initial-msg]
   (verb/say initial-msg)
   (loop [state (verb/change-rooms game-state (:current-room game-state))]
     (let [input (get-input)
           new-state (verb/process-input state input)]
       (if-not (finished? new-state)
         (recur new-state))))
   (verb/say "\nThe End.")))
