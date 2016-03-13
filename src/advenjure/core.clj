(ns advenjure.core
  (:require [advenjure.items :as item]
            [advenjure.rooms :as room]
            [advenjure.game :as game])
  (:gen-class))

;;; DEFINE ROOMS AND ITEMS
(def magazine (item/make ["sports magazine" "magazine"]
                         "The cover reads 'Sports Almanac 1950-2000'"
                         :read "Oh là là? Oh là là!?"
                         :take true))

(def bedroom (-> (room/make "Bedroom"
                            "A smelling bedroom. There was an unmade bed near the corner and a lamp by the bed. To the north there was a door."
                            :initial-description "I woke up in a smelling little bedroom, without windows or any furniture other than the bed I was laying in and a reading lamp. To the north there was a door.")
                 (room/add-item magazine "Laying by the bed was a sports magazine.") ; use this when describing the room instead of "there's a magazine here"
                 (room/add-item (item/make ["bed"] "It was the bed I slept in.") "") ; empty means skip it while describing, already contained in room description
                 (room/add-item (item/make ["reading lamp" "lamp"] "Nothing special about the lamp.") "")))


(def drawer (item/make ["drawer" "drawers" "chest" "chest drawer" "chest drawers" "drawer chest" "drawers chest"]
                       "A chest drawer"
                       :closed true
                       :items #{}))


(def living (room/make "Living Room" "A living room with a nailed shut window. A wooden door leads east."
                       :initial-description "The living room was as smelly as the bedroom, and although there was a window, it appeared to be nailed shut. A wooden door leads east.\nThere was a pretty good chance I'd choke to death if I didn't leave the place soon."))

(def outside (room/make "Outside" "I found myself in a beautiful garden and was able to breath again. A new adventure began, an adventure that is out of the scope of this example game."))

;;; BUILD THE ROOM MAP
(def room-map (-> {:bedroom bedroom
                   :living living
                   :outside outside}
                  (room/connect :bedroom :north :living)
                  (room/connect :living :east :outside)))


(defn -main
  "Build and run the example game."
  [& args]
  (let [game-state (game/make room-map :bedroom)
        finished? #(= (:current-room %) :outside)]
    (game/run game-state finished? "Welcome to the example game!")))
