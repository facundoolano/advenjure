(ns advenjure.change-rooms
  (:require [advenjure.rooms :as rooms]
            [advenjure.hooks :as hooks]
            [advenjure.utils :refer [say]]))

(defn change-rooms
  "Change room, say description, set visited."
  [game-state new-room]
  (let [previous (:current-room game-state)
        new-state (-> game-state
                    (assoc :previous-room previous)
                    (assoc :current-room new-room)
                    (hooks/execute :before-change-room))
        ; read from state again, in case hooks modified it
        room-spec (get-in new-state [:room-map (:current-room new-state)])
        ; set as visited when all of the rest done
        new-state (assoc-in new-state [:room-map new-room :visited] true)]
    (say (rooms/describe room-spec))
    (hooks/execute new-state :after-change-room)))

