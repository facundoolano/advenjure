(ns advenjure.verbs
  (:require [clojure.string :as str]
            [clojure.set :as clset]
            [advenjure.items :refer [iname describe-container get-from remove-from replace-from]]
            [advenjure.rooms :as rooms]))


;;; UTILITY functions

(defn current-room
  "Get the current room spec from game state."
  [game-state]
  (get-in game-state [:room-map (:current-room game-state)]))

(defn find-direction
  "Try to match the string with a direction. Allows synonyms: 'n', 'nw', etc."
  [token]
  (def mappings {"north" :north, "n" :north
                 "northeast" :northeast, "ne" :northeast
                 "east" :east, "e" :east
                 "southeast" :southeast, "se" :southeast
                 "south" :south, "s" :south
                 "southwest" :southwest, "sw" :southwest
                 "west" :west, "w" :west
                 "northwest" :northwest, "nw" :northwest})
  (get mappings token))


(defn find-item
    "Try to find the given item name either in the inventory or the current room."
    [game-state token]
    (or (get-from (:inventory game-state) token)
        (get-from (:items (current-room game-state)) token)))


(defn say
  ([speech] (println (str/capitalize speech))))

(defn item-or-message
  [game-state item verb-name]
  (if (empty? item)
    (say (str verb-name " what?"))
    (if-let [item-spec (find-item game-state item)]
      item-spec
      (say "I don't see that."))))

;;; VERB HANDLER FUNCTIONS
;;; Every handler takes the game state and the command tokens. If game state changes returns the new state, otherwise nil

(defn go
  "Change the location if direction is valid"
  [game-state direction]

  (defn change-rooms
    "Change room, say description, set visited."
    [new-room]
    (let [room-spec (get-in game-state [:room-map new-room])]
      (say (rooms/describe room-spec))
      (-> game-state
          (assoc :current-room new-room)
          (assoc-in [:room-map new-room :visited] true))))

  (if-let [dir (find-direction direction)]
    (if-let [new-room (dir (current-room game-state))]
     (change-rooms new-room)
     (say "Can't go in that direction"))
    (say "Go where?")))


(defn look
  "Look around (describe room). If tokens is defined, show error phrase."
  [game-state]
  (say (rooms/describe (current-room game-state))))

(defn look-at
  "Look at item."
  [game-state item]
  (if-let [item-spec (item-or-message game-state item "Look at")]
    (say (:description item-spec))))

(defn look-inside
  "Look inside container."
  [game-state container]
  (if-let [item (item-or-message game-state container "Look inside")]
    (if (:items item)
      (say (describe-container item))
      (say (str "I can't look inside a " (iname item) ".")))))

(defn take_
  "Try to take an item from the current room or from a container object in the inventory.
  Won't allow taking an object already in the inventory (i.e. not in a container)."
  [game-state item-name]
  (if-let [item (item-or-message game-state item-name "Take")]
    (let [inventory (:inventory game-state)]
      (cond
        (contains? inventory item) (say "I already got that.")
        (not (:take item)) (say "I can't take that.")
        :else (let [room-kw (:current-room game-state)
                    room (current-room game-state)]
                (say "Taken.")
                (-> game-state
                    (assoc :inventory (conj (remove-from inventory item) item))
                    (assoc-in [:room-map room-kw :items]
                              (remove-from (:items room) item))))))))


(defn open [game-state item-name]
  (if-let [item (item-or-message game-state item-name "Open")]
    (cond
      (nil? (:closed item)) (say "I can't open that.")
      (not (:closed item)) (say "It's already open.")
      :else (let [room-kw (:current-room game-state)
                  room (current-room game-state)
                  inventory (:inventory game-state)
                  open-item (assoc item :closed false)]
              (say "Opened.")
              (-> game-state
                    (assoc :inventory (replace-from inventory item open-item))
                    (assoc-in [:room-map room-kw :items]
                              (replace-from (:items room) item open-item)))))))

(defn close [game-state item-name]
  ; kind of copypasta, but well
  (if-let [item (item-or-message game-state item-name "Close")]
    (cond
      (nil? (:closed item)) (say "I can't close that.")
      (:closed item) (say "It's already closed.")
      :else (let [room-kw (:current-room game-state)
                  room (current-room game-state)
                  inventory (:inventory game-state)
                  closed-item (assoc item :closed true)]
              (say "Closed.")
              (-> game-state
                    (assoc :inventory (replace-from inventory item closed-item))
                    (assoc-in [:room-map room-kw :items]
                              (replace-from (:items room) item closed-item)))))))


;;; BUILD VERB MAP
(defn add-verb
  "Adds the given function as the handler for every verb in the list."
  [verb-map verbs handler]
  (merge verb-map (zipmap verbs (repeat handler))))


(def verb-map (-> {}
                  (add-verb ["^go (.*)"] go)
                  (add-verb ["^look$" "^look around$"] look)
                  (add-verb ["^look at (.*)" "^describe (.*)"] look-at)
                  (add-verb ["^take (.*)" "^get (.*)" "^pick (.*)" "^pick up (.*)"] take_)
                  (add-verb ["^inventory$" "^i$"] identity)
                  (add-verb ["^read (.*)"] identity)
                  (add-verb ["^open (.*)"] open)
                  (add-verb ["^close (.*)"] close)
                  (add-verb ["^turn on (.*)" "^turn (.*) on"] identity)
                  (add-verb ["^turn off (.*)" "^turn (.*) off"] identity)
                  (add-verb ["^put (.*) in (.*)" "^put (.*) inside (.*)"] identity)
                  (add-verb ["^unlock (.*) with (.*)"] identity) ; FIXME compound; FIXME open X with Y should work too
                  (add-verb ["^save$"] identity)
                  (add-verb ["^restore$" "^load$"] identity)
                  (add-verb ["^help$"] identity)))

;keep a sorted version to extract the longest possible form first
(def sorted-verbs (reverse (sort-by count (keys verb-map))))

(defn find-verb
  "Return [verb remaining] if there's a proper verb at the beginning of text."
  [text]
  (defn match-verb [verb]
    (let [[head & tokens :as full] (re-find (re-pattern verb) text)]
      (cond
        (and (not (nil? full)) (not (coll? full))) [verb '()] ;single match, no params
        (not-empty head) [verb tokens]))) ; match with params

  (some match-verb sorted-verbs))

(defn process-input
  "Take an input comand, find the verb in it and execute its action handler."
  [game-state input]
  (let [clean (str/trim (str/lower-case input))
        [verb tokens] (find-verb clean)
        handler (get verb-map verb)]
   (if handler
     (or (apply handler game-state tokens) game-state)
     (do (say "I don't know how to do that.") game-state))))
