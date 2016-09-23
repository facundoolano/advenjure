(ns advenjure.change-rooms
  (:require [advenjure.rooms :as rooms]
            [advenjure.utils :refer [say]]
            [advenjure.map :refer [print-map]]))

(defn change-rooms
  "Change room, say description, set visited."
  [game-state new-room]
  (let [room-spec (get-in game-state [:room-map new-room])
        new-state (-> game-state
                    (assoc :current-room new-room)
                    (assoc-in [:room-map new-room :visited] true))]
    (say (rooms/describe room-spec))
    (if-not (:visited room-spec) (print-map new-state))
    new-state))

