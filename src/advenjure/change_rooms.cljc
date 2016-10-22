(ns advenjure.change-rooms
  (:require [advenjure.rooms :as rooms]
            [advenjure.utils :refer [say]]
            [advenjure.ui.output :refer [clear]]
            [advenjure.map :refer [print-map_]]))

(defn change-rooms
  "Change room, say description, set visited."
  [game-state new-room]
  (let [room-spec (get-in game-state [:room-map new-room])
        previous (:current-room game-state)
        new-state (-> game-state
                    (assoc :previous-room previous)
                    (assoc :current-room new-room)
                    (assoc-in [:room-map new-room :visited] true))
        visited? (:visited room-spec)]
    (if-not visited? (clear))
    (say (rooms/describe room-spec))
    (if-not visited? (print-map_ new-state))
    new-state))

