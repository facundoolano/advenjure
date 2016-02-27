
(require ['clojure.string :as 'str])

;;; UTILITY functions

(defn current-room
  "Get the current room spec from game state."
  [game-state]
  (get-in state [:room-map (:current-room game-state)]))

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
  "Get the spec for the item with the given name, if it's in the given set."
  [item-set item]
  (first (filter #(some #{item} (:names %)) item-set)))


(def say
  ([speech] (say speech nil))
  ([speech game-state] (println speech) game-state))


;;; VERB HANDLER FUNCTIONS
;;; Every handler takes the game state and the command tokens, and returns the new game state

; FIXME use clojures for inlined functions
(defn go
  "Change the location if direction is valid"
  [game-state tokens]

  (def change-rooms
    "Change room, say description, set visited."
    [game-state new-room]
    (let [room-spec (get-in game-state [:room-map new-room])]
      (if (:visited room-spec)
        (say (:short-description :room-spec))
        (say (:full-description :room-spec)))
      (-> game-state
          (assoc :current-room new-room)
          (assoc-in [:room-map new-room :visited] true))))

  (if-let [dir (find-direction tokens)]
    (if-let [new-room (get-in game-state [:current-room dir])]
     (change-rooms game-state new-room)
     (say "Can't go in that direction" game-state))
    (say "Go where?" game-state)))

(defn look
  "Look at item. If no item look at room."
  [game-state tokens]

  (defn find-item
    "Try to find the given item name either in the inventory or the current room."
    [game-state token]
    (or (get-item-in (:inventory game-state) token)
        (get-item-in (:items (current-room game-state)) token)))

  (if (or (not tokens) (= tokens ""))
    (say (:short-description (current-room game-state)) game-state)
    (if-let [item (find-item game-state tokens)]
      (say (:description item) game-state)
      (say "I don't see that." game-state))))


(defn take_
  [game-state token]
  (if-let [item (get-item-in (current-room game-state) token)]
    (let [room-kw (:current-room game-state)
          room (current-room game-state)
          inventory (:inventory game-state)]
      (-> game-state
          (assoc :inventory (conj inventory item))
          (assoc-in [:room-map room-kw :items] (disj (:items room) item))))
    (say "I don't see that." game-state)))



;;; BUILD VERB MAP
(defn add-verb
  "Adds the given function as the handler for every verb in the list."
  [verb-map handler verbs]
  (merge verb-map (zipmap verbs (repeat handler))))


;FIXME handle "n" "nw" as specific forms of go
;TODO swap handler/verb list order in paramters
(def verb-map (-> {}
                  (add-verb go ["go"])
                  (add-verb look ["look" "look at"])
                  (add-verb identity ["read"])
                  (add-verb take_ ["take" "get" "pick" "pick up"])
                  (add-verb identity ["open"])
                  (add-verb identity ["close"])
                  (add-verb identity ["turn on"])
                  (add-verb identity ["turn off"])
                  (add-verb identity ["save"])
                  (add-verb identity ["restore" "load"])))

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
     (handler game-state tokens)
     (say "I don't know how to do that." game-state))))
