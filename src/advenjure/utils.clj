(ns advenjure.utils
  (:require [clojure.string :as string]
            [advenjure.items :refer :all]
            [advenjure.rooms :as rooms]
            [advenjure.ui.output :refer [print-line]]
            [gettext.core :refer [_]]))

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
                         (_ "northwest") :northwest, (_ "nw") :northwest})

(def directions (set (vals direction-mappings)))

(defn find-item
  "Try to find the given item name either in the inventory or the current room."
  [game-state token]
  (concat (get-from (:inventory game-state) token)
          (get-from (:items (current-room game-state)) token)))

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

(defn say
  [speech]
  (print-line
    (string-wrap (str (string/capitalize (first speech))
                      (subs speech 1)))))
