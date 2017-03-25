(ns advenjure.plugins.points
  "Plugin that allows adding and showing score points on given actions are
  performed or rooms visited."
  (:require [advenjure.utils :as utils]))

(defn points-prompt [gs]
  (str "\n@" (:name (utils/current-room gs))
       " [P:" (or (:points gs) 0) " | M:" (:moves gs) "] > "))

(defn add-points [gs amount]
  (assoc gs :points (+ (or (:points gs) 0) amount)))

;; TODO check how this plays together with weird verbs like unlock
(defn add-verb-points
  "If the item has points for the given verb, apply them and remove"
  [gs kw & [item]]
  (if-let [points (get-in item [kw :points])]
    (-> gs
        (add-points points)
        (utils/replace-item item (update item kw dissoc :points)))))

(defn add-room-points
  "If the room is visited for the first time and has a :points key, add them
  to the current score."
  [gs]
  (let [{visited :visited points :points} (utils/current-room gs)]
    (if (and (not visited) points)
      (add-points gs points))))

;; Plugin definition
(def points {:hooks {:after-item-handler add-verb-points
                     :before-change-room add-room-points}
             :prompt points-prompt})
