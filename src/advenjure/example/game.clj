(ns advenjure.example.game
  (:require [advenjure.items :as item]
            [advenjure.rooms :as room]
            [advenjure.utils :as utils]
            [advenjure.game :as game]
            [advenjure.dialogs :refer [dialog]]
            [advenjure.example.dialogs :refer [npc-dialog]])
  (:gen-class))

;;; DEFINE ROOMS AND ITEMS
(def magazine (item/make ["sports magazine" "magazine"]
                         "The cover reads 'Sports Almanac 1950-2000'"
                         :read "Oh là là? Oh là là!?"
                         :take true))

(def wallet (item/make ["wallet"] "It's made of cheap imitation leather."
                       :take true
                       :open "I don't have a dime."
                       :look-in "I don't have a dime."
                       :dialog `(dialog ("ME" "Hi, wallet.")
                                        ("WALLET" "Tsup?")
                                        ("ME" "Any cash I can use?")
                                        ("WALLET" "Sorry."))))

(def bedroom (-> (room/make "Bedroom"
                            "A smelling bedroom. There was an unmade bed near the corner and a door to the north."
                            :initial-description "I woke up in a smelling little bedroom, without windows. By the bed I was laying in was a small table and to the north a glass door.")
                 (room/add-item magazine "On the floor was a sports magazine.") ; use this when describing the room instead of "there's a magazine here"
                 (room/add-item (item/make ["bed"] "It was the bed I slept in.") "") ; empty means skip it while describing, already contained in room description
                 (room/add-item (item/make ["glass door" "door"]) "")
                 (room/add-item (item/make ["small table" "table"] "A small bed table."
                                           :items #{wallet (item/make ["reading lamp" "lamp"])}))))

(def door (item/make ["door" "wooden door"] "Just a wooden door." :locked true))

(def drawer (item/make ["chest drawer" "chest" "drawer"]
                       "It has one drawer."
                       :closed true
                       :items #{(item/make ["bronze key" "key"] "A bronze key." :unlocks door :take true)}))

(def living (-> (room/make "Living Room"
                           "A living room with a nailed shut window. A wooden door leaded east and a hallway south."
                           :initial-description "The living room was as smelly as the bedroom, and although there was a window, it appeared to be nailed shut. There was a pretty good chance I'd choke to death if I didn't leave the place soon.\nA wooden door leaded east and a hallway south.")
                (room/add-item drawer "There was a chest drawer by the door.")
                (room/add-item door "")
                (room/add-item (item/make ["window"] "It's nailed shut." :closed true :open "It's nailed shut.") "")))

(def npc (item/make ["character" "suspicious looking character" "npc"]
                    "The guy was fat and hairy and was giving me a crooked look." :dialog `npc-dialog))

(def hallway (-> (room/make "Hallway"
                            "A narrow hallway with a door to the west and a big portal to the east."
                            :initial-description "I walked out of the living room and found myself in the west side of a narrow hallway leading to a big portal towards east. I felt closer to the exit.")
                 (room/add-item (item/make "portal") "")
                 (room/add-item (item/make "door") "")
                 (room/add-item npc "A suspicious looking character was guarding the portal.")))

(def outside (room/make "Outside" "I found myself in a beautiful garden and was able to breath again. A new adventure began, an adventure that is out of the scope of this example game."))

(defn can-leave? [gs]
  (let [door (utils/find-item gs "wooden door")]
    (cond (:locked door) "The door is locked."
          (not (contains? (:inventory gs) wallet)) "I can't leave without my wallet."
          :else :hallway)))

(defn npc-gone? [gs]
  (if (utils/find-item gs "character")
    "I can't, that guy is blocking the portal."
    :outside))

;;; BUILD THE ROOM MAP
(def room-map (-> {:bedroom bedroom
                   :living living
                   :outside outside
                   :hallway hallway}
                  (room/connect :bedroom :north :living)
                  (room/one-way-connect :living :east `can-leave?)
                  (room/one-way-connect :hallway :west :living)
                  (room/one-way-connect :hallway :east `npc-gone?)))

; RUN THE GAME
(defn -main
  "Build and run the example game."
  [& args]
  (let [game-state (game/make room-map :bedroom)
        finished? #(= (:current-room %) :outside)]
    (game/run game-state finished? "Welcome to the example game! type 'help' if you don't know what to do.\n")))
