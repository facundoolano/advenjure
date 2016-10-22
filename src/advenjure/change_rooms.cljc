(ns advenjure.change-rooms
  (:require [advenjure.rooms :as rooms]
            [advenjure.utils :refer [say]]
            [advenjure.map :refer [print-map_]]))

(defn change-rooms
  "Change room, say description, set visited."
  [game-state new-room]
  (let [room-spec (get-in game-state [:room-map new-room])
        previous (:current-room game-state)
        new-state (-> game-state
                    (assoc :previous-room previous)
                    (assoc :current-room new-room)
                    (assoc-in [:room-map new-room :visited] true))]
    (say (rooms/describe room-spec))
    (if-not (:visited room-spec) (print-map_ new-state))
    new-state))

