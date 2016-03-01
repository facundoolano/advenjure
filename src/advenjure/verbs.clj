(ns advenjure.verbs
  (:require [clojure.string :as str]
            [advenjure.items :refer [iname describe-container]]
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

(defn get-item-in
  "Get the spec for the item with the given name, if it's in the given set,
  or is contained by one of its items."
  ; TODO probably a cleaner way to get this result
  ; TODO allow multiple results --i.e. door --> "glass door" "wooden door"
  [item-set item]
  (or (first (filter #(some #{item} (:names %)) item-set))
      (first (map #(get-item-in (:items %) item)
                  (filter #(and (not (:closed %)) (:items %)) item-set)))))

(defn find-item
    "Try to find the given item name either in the inventory or the current room."
    [game-state token]
    (or (get-item-in (:inventory game-state) token)
        (get-item-in (:items (current-room game-state)) token)))


(defn say
  ([speech] (println (str/capitalize speech))))


;;; VERB HANDLER FUNCTIONS
;;; Every handler takes the game state and the command tokens. If game state changes returns the new state, otherwise nil

(defn go
  "Change the location if direction is valid"
  [game-state tokens]

  (defn change-rooms
    "Change room, say description, set visited."
    [new-room]
    (let [room-spec (get-in game-state [:room-map new-room])]
      (say (rooms/describe room-spec))
      (-> game-state
          (assoc :current-room new-room)
          (assoc-in [:room-map new-room :visited] true))))

  (if-let [dir (find-direction tokens)]
    (if-let [new-room (dir (current-room game-state))]
     (change-rooms new-room)
     (say "Can't go in that direction"))
    (say "Go where?")))


(defn look
  "Look around (describe room). If tokens is defined, show error phrase."
  [game-state tokens]
  (if (or (not tokens) (= tokens ""))
    (say (rooms/describe (current-room game-state)))
    (say "I understood as far as 'look'")))

(defn look-at
  "Look at item."
  [game-state tokens]
  (if (or (not tokens) (= tokens ""))
    (say "Look at what?")
    (if-let [item (find-item game-state tokens)]
      (say (:description item))
      (say "I don't see that."))))

(defn look-inside
  "Look inside container."
  [game-state tokens]
  (if (or (not tokens) (= tokens ""))
    (say "Look inside what?")
    (if-let [item (find-item game-state tokens)]
      (if (:items item)
        (say (describe-container item))
        (say (str "I can't look inside a " (iname item) ".")))
      (say "I don't see that."))))


; TODO if in inventory -> "I already got it."
; TODO "Taken."
; TODO remove-in method
(defn take_
  [game-state token]
  (if-let [item (get-item-in (current-room game-state) token)]
    (let [room-kw (:current-room game-state)
          room (current-room game-state)
          inventory (:inventory game-state)]
      (-> game-state
          (assoc :inventory (conj inventory item))
          (assoc-in [:room-map room-kw :items] (disj (:items room) item))))
    (say "I don't see that.")))



;;; BUILD VERB MAP
(defn add-verb
  "Adds the given function as the handler for every verb in the list."
  [verb-map verbs handler]
  (merge verb-map (zipmap verbs (repeat handler))))


(def verb-map (-> {}
                  (add-verb ["go"] go) ;FIXME handle "n" "nw" as specific forms of go
                  (add-verb ["look"] look)
                  (add-verb ["look at" "describe"] look-at)
                  (add-verb ["take" "get" "pick" "pick up"] take_)
                  (add-verb ["inventory" "i"] identity)
                  (add-verb ["read"] identity)
                  (add-verb ["open"] identity)
                  (add-verb ["close"] identity)
                  (add-verb ["turn on"] identity)
                  (add-verb ["turn off"] identity)
                  (add-verb ["put"] identity)
                  (add-verb ["unlock"] identity) ; FIXME compound; FIXME open X with Y should work too
                  (add-verb ["save"] identity)
                  (add-verb ["restore" "load"] identity)
                  (add-verb ["help"] identity)))

;keep a sorted version to extract the longest possible form first
(def sorted-verbs (reverse (sort-by count (keys verb-map))))

(defn find-verb
  "Return [verb remaining] if there's a proper verb at the beginning of text."
  [text]
  (defn split-verb [verb]
    (let [[head tokens] (str/split text (re-pattern verb))]
      (if (= head "") ;found verb in text
        [verb (and tokens (str/trim tokens))])))

  (some split-verb sorted-verbs))

(defn process-input
  "Take an input comand, find the verb in it and execute its action handler."
  [game-state input]
  (let [clean (str/trim (str/lower-case input))
        [verb tokens] (find-verb clean)
        handler (get verb-map verb)]
   (if handler
     (or (handler game-state tokens) game-state)
     (say "I don't know how to do that."))))
