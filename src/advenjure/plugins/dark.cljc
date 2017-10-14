(ns advenjure.plugins.dark
  "Plugin that allows marking a room as :dark, in which case a :lit source
  is required to display it. If missing, a dark version of the room is presented
  instead."
  (:require [advenjure.rooms :as rooms]
            [advenjure.items :as items]
            [advenjure.change-rooms :refer [change-rooms]]
            [advenjure.utils :refer [directions current-room]]
            [advenjure.gettext.core :refer [_]]))

(defn build-dark-room
  "Make a dark version of the current room, preserving only the connection to the
  previous one."
  [game-state]
  (let [room-spec (current-room game-state)
        previous-kw (:previous-room game-state)
        rname (get room-spec :dark-name (:name room-spec))
        description (get room-spec :dark-description (_ "It was completely dark."))
        is-known-connection (fn [[k v]] (and (directions k) (= v previous-kw)))
        known-connections (filter is-known-connection room-spec)]
    (merge (rooms/make rname description)
           known-connections
           {:original-room (:current-room game-state)})))

(defn light-available?
  "Returns true if the room is dark and there are no light sources available."
  [game-state]
  (let [room (current-room game-state)
        items (items/all-items (into (:inventory game-state) (:items room)))
        light-items (filter :lit items)]
    (not-empty light-items)))

(defn hook
  "If this is a regular room marked as :dark and there's no light source, replace
  it with a generated dark copy of the room.
  If this is a generated dark copy of a room, and there's a light source, replace
  it with the regular room."
  [game-state]
  (let [room (current-room game-state)
        light? (light-available? game-state)
        is-generated-room (= :__dark-room (:current-room game-state))]
    (cond
      (and (:dark room) (not light?)) (let [dark-room (build-dark-room game-state)]
                                          (-> game-state
                                            (assoc-in [:room-map :__dark-room] dark-room)
                                            (assoc :current-room :__dark-room)))

      (and is-generated-room light?) (-> game-state
                                      (update-in [:room-map] dissoc :__dark-room)
                                      ; set previous as current so BACK verb doesnt do weird stuff ; FIXME doesnt seem to work
                                      (assoc :current-room (:previous-room game-state))
                                      (change-rooms (:original-room room))))))


; Plugin definition
(def dark-room {:hooks {:before-change-room hook
                        :after-handler hook}})
