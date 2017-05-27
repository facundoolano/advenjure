(ns advenjure.utils
  (:require [clojure.string :as string]
            [advenjure.items :refer [get-from remove-from replace-from]]
            [advenjure.rooms :as rooms]
            [advenjure.gettext.core :refer [_]]))

(defn current-room
  "Get the current room spec from game state."
  [game-state]
  (get-in game-state [:room-map (:current-room game-state)]))

(def direction-mappings {(_ "north") :north, (_ "n") :north
                         (_ "northeast") :northeast, (_ "ne") :northeast
                         (_ "east") :east, (_ "e") :east
                         (_ "southeast") :southeast, (_ "se") :southeast
                         (_ "south") :south, (_ "s") :south
                         (_ "southwest") :southwest, (_ "sw") :southwest
                         (_ "west") :west, (_ "w") :west
                         (_ "northwest") :northwest, (_ "nw") :northwest
                         (_ "up") :up, (_ "u") :up
                         (_ "down") :down, (_ "d") :down})

(def direction-names {:north (_ "north")
                      :northeast (_ "northeast")
                      :east (_ "east")
                      :southeast (_ "southeast")
                      :south (_ "south")
                      :southwest (_ "southwest")
                      :west (_ "west")
                      :northwest (_ "northwest")
                      :up (_ "up")
                      :down (_ "down")})

(def directions (set (vals direction-mappings)))

(defn get-visible-room
  "Returns the spec of the room by the given name, if it's connected and visible
  (i.e. known or visited) current one."
  [game-state room-name]
  (let [current (:current-room game-state)
        rmap (:room-map game-state)
        name-mappings (rooms/visible-name-mappings rmap current)
        room-name (if room-name (string/lower-case room-name))]
    (get name-mappings room-name)))

(defn room-as-item
  "Adapt the room record to be used as an item. For now it just removes :items
  to avoid conflicts."
  [room]
  (-> room
      (assoc :names (rooms/names room))
      (dissoc :items)))

(defn find-item
  "Try to find the given item name either in the inventory or the current room."
  [game-state token]
  (concat (get-from #{(room-as-item (current-room game-state))} token)
          (get-from (:inventory game-state) token)
          (get-from (:items (current-room game-state)) token)))

(def find-first (comp first find-item))

(defn remove-item
  [game-state item]
  (let [room-kw (:current-room game-state)
        room (current-room game-state)
        inventory (:inventory game-state)]
    (-> game-state
        (assoc :inventory (remove-from inventory item))
        (assoc-in [:room-map room-kw :items]
                  (remove-from (:items room) item)))))

(defn replace-item
  [game-state old-item new-item]
  (let [room-kw (:current-room game-state)
        room (current-room game-state)
        inventory (:inventory game-state)]
    (-> game-state
        (assoc :inventory (replace-from inventory old-item new-item))
        (assoc-in [:room-map room-kw :items]
                  (replace-from (:items room) old-item new-item)))))

;; This should be done only when printing
(defn capfirst
  "Converts first character to upper-case, leaves the rest untouched."
  [s]
  (if (string/blank? s)
    s
    (str (string/capitalize (first s)) (subs s 1))))

(defn clear-screen [gs]
  (update gs :out str "\n__CLEAR__\n"))

(defn clear? [s] (= s "__CLEAR__"))

;; TODO trim, if last not whitespace and not punctuation, add a dot
(defn say
  "Add speech to the last unfinished line and finish it with \n."
  [gs speech]
  (update gs :out str speech "\n"))

(defn say-inline
  "Add speech to the last unfinished line."
  [gs speech]
  (update gs :out str speech))
