(ns advenjure.rooms
  (:require [advenjure.items :refer [iname describe-container print-list-item]]
            [clojure.string :as string]
            [advenjure.gettext.core :refer [_]]))

(defrecord Room [name description])

(defn make [name description & {:as extras}]
  (map->Room (merge {:name name :description description}
                    {:items #{} :item-descriptions {} :visited false} ;default args, can be overriden by extras
                    extras)))

(defn add-item
  "Add the item to the room with an optional custom description.
  Returns the updated room."
  ([room item]
   (update-in room [:items] conj item))
  ([room item description]
   (-> room
       (update-in [:items] conj item)
       (assoc-in [:item-descriptions (iname item)] description))))

(defn describe
  "Describes the room.
  If the room was not visited and there's an initial-description supplied use
  it, otherwise use the default description.
  The items contained in the room will be listed. If there are custom item
  descriptions defined use them instead of the default list description."
  [room]
  (let [room-descr (if (:visited room)
                     (:description room)
                     (or (:initial-description room) (:description room)))

        current-items (set (map iname (:items room))) ;items currently in the room
        custom-items (filter #(current-items (first %)) (:item-descriptions room)) ;items in the room with custom description
        custom-descr (string/join " " (remove string/blank? (vals custom-items))) ; string of custom descriptions (skip if blank)
        custom-prefix (if (not-empty custom-descr) " ") ; put a space between room and custom descriptions if any
        remain-items (filter #(nil? (get (:item-descriptions room) (iname %))) (:items room)) ; get room items with no custom description
        item-descr (reduce #(str %1 (_ "\nThere was %s there." (print-list-item %2))
                                 (describe-container %2 " "))
                           ""
                           remain-items)]

    (if (not-empty item-descr)
      (str room-descr custom-prefix custom-descr item-descr)
      (str room-descr custom-prefix custom-descr))))

; ROOM MAP BUILDING
(def matching-directions {:north :south
                          :northeast :southwest
                          :east :west
                          :southeast :northwest
                          :south :north
                          :southwest :northeast
                          :west :east
                          :northwest :southeast
                          :up :down
                          :down :up})

(def directions (set (vals matching-directions)))

(defn connect
  "Connect r1 with r2 in the given direction and make the corresponding
  connection in r2."
  [room-map r1 direction r2]
  (-> room-map
      (assoc-in [r1 direction] r2)
      (assoc-in [r2 (get matching-directions direction)] r1)))

(defn one-way-connect
  "Connect r1 with r2 without the corresponding connection in r2. Note that
  r2 can be a condition function or string to make a conditional connection."
  [room-map r1 direction r2]
  (assoc-in room-map [r1 direction] r2))


(defn connection-dir
  "Check if the given room has a connection to a room with the given kw. If it
  does, return the direction."
  [room-spec roomkw]
  (some #(and (= (% room-spec) roomkw) %) directions))


