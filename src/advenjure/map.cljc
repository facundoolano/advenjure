(ns advenjure.map
  (:require [clojure.string :as string]
            [advenjure.utils :refer [current-room directions]]
            [advenjure.ui.output :refer [print-line]]
            [advenjure.conditions :refer [eval-precondition]]))

(def full 60)
(def half (/ full 2))
(def square (count "+--- N ---+"))
(def side (/ (- full square) 2))

(defn pad
  "Sorround the given text with spaces so the total size is the requested"
  [size text]
  (let [text (or text " ")
        total-space (- size (count text))
        single-space (inc (int (/ total-space 2)))
        spaces (string/join (repeat single-space " "))]
    (str spaces text spaces)))

(defn assoc-room-name
  "Add the name of the room in the given direction to the name map. Uses '?'
  for unvisited rooms and 'X' for blocked rooms."
  [game-state name-map [dir condition]]
  (let [room-kw (eval-precondition condition game-state)
        room (get-in game-state [:room-map room-kw])
        rname (cond (string? room-kw) "X"
                    (:visited room) (:name room)
                    :else "?")]
    (assoc name-map dir rname)))

(defn room-names
  [game-state]
  (let [current (current-room game-state)
        connected-kws (select-keys current directions)]
    (reduce #(assoc-room-name game-state %1 %2) {} connected-kws)))

(defn print-map
  "Print a map of the current room, with the connections in every direction."
  [game-state]
  (let [rooms (room-names game-state)]
    (print-line)
    (print-line (pad full (:north rooms)))
    (print-line (str (pad half (:northwest rooms)) (pad half (:northeast rooms))))
    (print-line (pad full "+--- N ---+"))
    (print-line (pad full "|         |"))
    (print-line (str (pad side (:west rooms))  "W         E" (pad side (:east rooms))))
    (print-line (pad full "|         |"))
    (print-line (pad full "+--- S ---+"))
    (print-line (str (pad half (:southwest rooms)) (pad half (:southeast rooms))))
    (print-line (pad full (:south rooms)))))
