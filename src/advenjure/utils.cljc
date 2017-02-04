(ns advenjure.utils
  (:require [clojure.string :as string]
            [advenjure.items :refer [get-from remove-from replace-from]]
            [advenjure.rooms :as rooms]
            #?(:cljs [advenjure.ui.output :refer [print]])
            [advenjure.ui.output :refer [print-line]]
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

(def directions (set (vals direction-mappings)))

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

(defn string-wrap
  ([text] (string-wrap text 80))
  ([text max-size]
   (loop [[word & others] (string/split text #" ")
           current ""
           lines []]
     (if word
       (let [new-current (str current " " word)
             line-size (count (last (string/split new-current #"\n")))]
         (if (> max-size line-size)
           (recur others new-current lines)
           (recur others word (conj lines current))))
       (string/triml (string/join "\n" (conj lines current)))))))

(defn capfirst
  "Converts first character to upper-case, leaves the rest untouched."
  [s]
  (str (string/capitalize (first s)) (subs s 1)))

(defn say
  [speech]
  (print-line (string-wrap (capfirst speech))))

(defn say-inline
  [speech]
  (print (str (capfirst speech))))
