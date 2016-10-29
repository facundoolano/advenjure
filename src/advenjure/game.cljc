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
    :moves 0}))

(defn process-input
  "Take an input comand, find the verb in it and execute its action handler."
  [verb-map game-state input]
  (let [clean (clojure.string/trim (clojure.string/lower-case input))
        [verb tokens] (find-verb verb-map clean)
        handler (get verb-map verb)]
    (if handler
      (alet [new-state (update-in game-state [:moves] inc)
             handler-state (apply handler new-state tokens)]
        (or handler-state new-state))

      (do (if-not (clojure.string/blank? clean) (print-line (_ "I didn't know how to do that.")))
        game-state))))

(defn run
  "Run the game loop. Requires a finished? function to decide when to terminate the loop."
  ([game-state finished?] (run game-state finished? ""))
  ([game-state finished? initial-msg]
   (run game-state finished? initial-msg default-map))
  ([game-state finished? initial-msg verb-map]
   (init)
   (utils/say initial-msg)
   (alet [k (read-key)]
     (aloop [state (change-rooms game-state (:current-room game-state))]
       (let!? [input (get-input state verb-map)
               new-state (process-input verb-map state input)]
         (if-not (finished? new-state)
           (recur new-state)
           (do
            (print-line (_ "\nThe End."))
            (exit))))))))
