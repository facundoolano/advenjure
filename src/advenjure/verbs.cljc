(ns advenjure.verbs
  #?(:cljs (:require-macros [advenjure.async :refer [alet]]))
  (:require [clojure.set]
            [advenjure.utils :refer [say say-inline find-item direction-mappings
                                     current-room remove-item replace-item capfirst
                                     directions direction-names get-visible-room]]
            [advenjure.change-rooms :refer [change-rooms]]
            [advenjure.hooks :refer [execute eval-precondition eval-postcondition
                                     eval-direction]]
            [advenjure.items :refer [print-list describe-container iname all-items]]
            [advenjure.rooms :as rooms]
            [advenjure.gettext.core :refer [_ p_]]
            [advenjure.ui.input :as input :refer [read-file]]
            [advenjure.ui.output :refer [write-file]]
            #?(:clj [advenjure.async :refer [alet]])
            #?(:cljs [advenjure.eval :refer [eval]])))

;;;; FUNCTIONS TO BUILD VERB HANDLERS

; the noop handler does nothing except optionally saying something, the item specific
; behavior is defined in the item spec
(defn noop [kw]
  (fn [gs item & etc]
    (if-let [speech (get-in item [kw :say])]
      (say speech))))

(defn ask-ambiguous
  [item-name items]
  (let [first-different (fn [spec] (first (filter #(not= % item-name) (:names spec))))
        names (map first-different items)
        names (map #(str "the " %) names)
        first-names (clojure.string/join ", " (butlast names))]
    (str "Which " item-name "? "
      (capfirst first-names) " or " (last names) "?")))

(defn make-item-handler
  "Takes the verb name, the kw to look up at the item at the handler function,
  wraps the function with the common logic such as trying to find the item,
  executing pre/post conditions, etc."
  ; this one uses a noop handler, solely based on post/preconditions (i.e. read)
  ([verb-name verb-kw] (make-item-handler verb-name verb-kw (noop verb-kw)))
  ([verb-name verb-kw handler &
    {:keys [kw-required] :or {kw-required true}}]
   (fn
     ([game-state] (say (_ "%s what?" verb-name)))
     ([game-state item-name]
      (alet [items (find-item game-state item-name)
             item (first items)
             conditions (verb-kw item)
             value (eval-precondition conditions game-state)]
        (cond
          (empty? items) (say (_ "I didn't see that."))
          (> (count items) 1) (say (ask-ambiguous item-name items))
          (string? value) (say value)
          (false? value) (say (_ "I couldn't %s that." verb-name))
          (and kw-required (nil? value)) (say (_ "I couldn't %s that." verb-name))
          :else (alet [before-state (execute game-state :before-item-handler verb-kw)
                       handler-state (handler before-state item)
                       post-state (eval-postcondition conditions before-state handler-state)]
                  (execute post-state :after-item-handler verb-kw))))))))


(defn make-compound-item-handler
  ; some copy pasta, but doesn't seem worth to refactor
  "The same as above but adapted to compund verbs."
  ([verb-name verb-kw] (make-compound-item-handler verb-name verb-kw (noop verb-kw)))
  ([verb-name verb-kw handler &
    {:keys [kw-required] :or {kw-required true}}]
   (fn
     ([game-state] (say (_ "%s what?" verb-name)))
     ([game-state item1] (say (_ "%s %s with what?" verb-name item1)))
     ([game-state item1-name item2-name]
      (alet [items1 (find-item game-state item1-name)
             item1 (first items1)
             items2 (find-item game-state item2-name)
             item2 (first items2)
             conditions (verb-kw item1)
             value (eval-precondition conditions game-state item2)]
        (cond
          (or (empty? items1) (empty? items2)) (say (_ "I didn't see that."))
          (> (count items1) 1) (say (ask-ambiguous item1-name items1))
          (> (count items2) 1) (say (ask-ambiguous item2-name items2))
          (string? value) (say value)
          (false? value) (say (str "I couldn't " verb-name " that."))
          (and kw-required (nil? value)) (say (_ "I couldn't %s that." verb-name))
          :else (alet [before-state (execute game-state :before-item-handler verb-kw)
                       handler-state (handler before-state item1 item2)
                       post-state (eval-postcondition conditions before-state handler-state)]
                  (execute post-state :after-item-handler verb-kw))))))))


;;; VERB HANDLER DEFINITIONS
(defn go
  "Change the location if direction is valid."
  ([game-state] (say (_ "Go where?")))
  ([game-state direction]
   (let [current (current-room game-state)]
     (if-let [dir (get direction-mappings direction)]
       (alet [dir-value (eval-direction game-state dir)]
             (cond
               (string? dir-value) (say dir-value)
               (not dir-value) (say (or (:default-go current) (_ "Couldn't go in that direction.")))
               :else (alet [new-state (change-rooms game-state dir-value)]
                           (eval-postcondition (dir current) game-state new-state))))
       ;; it's not a direction name, maybe it's a room name
       (if-let [roomkw (get-visible-room game-state direction)]
         (change-rooms game-state roomkw)
         (say "Go where?"))))))

(defn look-to
  "Describe what's in the given direction."
  ([game-state] (say (_ "Look to where?")))
  ([game-state direction]
   (if-let [dir (get direction-mappings direction)]
     (alet [dir-value (eval-direction game-state dir)
            dir-room (get-in game-state [:room-map dir-value])]
           (cond
             (string? dir-value) (say (_ "That direction was blocked."))
             (not dir-value) (say (_ "There was nothing in that direction."))
             (or (:known dir-room) (:visited dir-room)) (say (_ "The %s was in that direction." (:name dir-room)))
             :else (say (_ "I didn't know what was in that direction."))))
     ;; it's not a direction name, maybe it's a room name
     (if-let [roomkw (get-visible-room game-state direction)]
       (let [room-name (get-in game-state [:room-map roomkw :name])
             ;; this feels kinda ugly:
             dir-kw (roomkw (clojure.set/map-invert (current-room game-state)))
             dir-name (dir-kw direction-names)]
        (say (_ "The %s was toward %s." room-name dir-name)))
       (say (_ "Look to where?"))))))

(defn go-back
  "Go to the previous room, if possible."
  [game-state]
  (if-let [roomkw (:previous-room game-state)]
    (if (rooms/connection-dir (current-room game-state) roomkw)
      (change-rooms game-state roomkw)
      (say (_ "Where would back be?")))
    (say (_ "Where would back be?"))))

(defn look
  "Look around (describe room) and enumerate available movement directions."
  [game-state]
  (say (str (rooms/describe (current-room game-state))))
  (say " ")
  (reduce (fn [gs dirkw]
            (let [dir-value (eval-direction game-state dirkw)
                  dir-name (dirkw direction-names)
                  dir-room (get-in game-state [:room-map dir-value])]
              (if dir-value
                (alet [gs gs ; wait for the channel value before printing the next item name
                       unused (say-inline (str dir-name ": "))]
                      (cond
                        (string? dir-value) (say (_ "blocked."))
                        (or (:visited dir-room) (:known dir-room)) (say (str (:name dir-room) "."))
                        :else (say "???"))
                      gs)
                gs)))
          game-state
          directions))


(defn inventory
  "Describe the inventory contents."
  [game-state]
  (if (empty? (:inventory game-state))
    (say (_ "I wasn't carrying anything."))
    (say (str (_ "I was carrying:") (print-list (:inventory game-state))))))

(defn save
  "Save the current game state to a file."
  [game-state]
  (write-file "saved.game" (dissoc game-state :configuration))
  (say (_ "Done.")))

(defn restore
  "Restore a previous game state from file."
  [game-state]
  (try
    (let [loaded-state (assoc (read-file "saved.game") :configuration (:configuration game-state))]
      (say (rooms/describe (current-room loaded-state)))
      loaded-state)
    (catch #?(:clj java.io.FileNotFoundException :cljs js/Object) e (say (_ "No saved game found.")))))

(defn exit
  "Close the game."
  [game-state]
  (say (_ "Bye!"))
  (input/exit))

(def look-at
  (make-item-handler
   (_ "look at") :look-at
   (fn [game-state item] (say (:description item)))
   :kw-required false))

(def look-inside
  (make-item-handler
   (_ "look inside") :look-in
   (fn [game-state item]
    (if-let [custom-say (get-in item [:look-in :say])]
      (say custom-say)
      (if (:items item)
        (say (describe-container item))
        (say (_ "I couldn't look inside a %s." (iname item))))))
   :kw-required false))

(def take_
  (make-item-handler
   (_ "take") :take
   (fn [game-state item]
     "Try to take an item from the current room or from a container object in the inventory.
      Won't allow taking an object already in the inventory (i.e. not in a container)."
     (if (contains? (:inventory game-state) item)
       (say (_ "I already had that."))
       (let [new-state (remove-item game-state item)
             new-inventory (conj (:inventory new-state) item)]
         (say (get-in item [:take :say] (_ "Taken.")))
         (assoc new-state :inventory new-inventory))))))

(defn take-all
  "Go through every item in the room that defines a value for :take, and attempt
  to take it."
  [game-state]
  (let [items (all-items (:items (current-room game-state)))
        takeable (remove (comp nil? :take) items)
        item-names (map #(first (:names %)) takeable)]
    (if (empty? item-names)
      (say (_ "I saw nothing worth taking."))
      (reduce (fn [gs iname]
                (alet [gs gs ; wait for the channel value before printing the next item name
                       _ (say-inline (str iname ": "))
                       new-state (take_ gs iname)
                       result (or new-state gs)]
                  result))
         game-state item-names))))

(def open
  (make-item-handler
   (_ "open") :open
   (fn [game-state item]
     (cond
       (not (:closed item)) (say (p_ item "It was already open."))
       (:locked item) (say (p_ item "It was locked."))
       :else (let [open-item (assoc item :closed false)
                   custom-say (get-in item [:open :say])]
               (cond
                custom-say (say custom-say)
                (:items open-item) (say (describe-container open-item))
                :else (say (_ "Opened.")))
               (replace-item game-state item open-item))))))

(def close
  (make-item-handler
   (_ "close") :close
   (fn [game-state item]
     (if (:closed item)
       (say (p_ item "It was already closed."))
       (let [closed-item (assoc item :closed true)]
         (say (get-in item [:close :say] (_ "Closed.")))
         (replace-item game-state item closed-item))))))

(def unlock
  (make-compound-item-handler
   (_ "unlock") :unlock
   (fn [game-state locked key-item]
     (cond
       (not (:locked locked)) (say (p_ locked "It wasn't locked."))
       (not= locked (:unlocks key-item)) (say (_ "That didn't work."))
       :else (let [unlocked (assoc locked :locked false)]
               (say (get-in locked [:unlock :say] (_ "Unlocked.")))
               (replace-item game-state locked unlocked))))))

(def open-with
  (make-compound-item-handler
   (_ "open") :open-with
   (fn [game-state closed key-item]
     "An unlock + open of sorts."
     (cond
       (not (:closed closed)) (say (p_ closed "It was already open."))
       (or (and (:locked closed) (= closed (:unlocks key-item)))
           (= closed (:opens key-item))) (let [opened (merge closed {:locked false :closed false})]
                                          (say (get-in closed [:open-with :say] (_ "Opened.")))
                                          (replace-item game-state closed opened))
       :else (say (_ "That didn't work."))))
   :kw-required false))

(def talk
  (make-item-handler
   (_ "talk to") :talk
   (fn [game-state item]
     (let [dialog (eval (:dialog item))]
       (dialog game-state)))))

;;; VERBS THAT USE ITEMS TO CHANGE ROOMS
(defn make-move-item-handler
  [verb-name verb-kw]
  (make-item-handler verb-name verb-kw
    (fn [game-state item]
      (change-rooms game-state (eval-precondition (verb-kw item))))))

(def climb (make-move-item-handler (_ "climb") :climb))
(def climb-up (make-move-item-handler (_ "climb up") :climb-up))
(def climb-down (make-move-item-handler (_ "climb down") :climb-down))
(def enter (make-move-item-handler (_ "enter") :enter))

;;; NOOP VERBS (rely entirely in pre/post conditions)
(def read_ (make-item-handler (_ "read") :read))
(def use_ (make-item-handler (_ "use") :use))

(def use-with (make-compound-item-handler (_ "use") :use-with))

;; SAY VERBS
(defn make-say-verb [speech]
  (fn [gs] (say speech)))

(def stand (make-say-verb (_ "I was standing up already")))
(def help (make-say-verb (clojure.string/join "\n    " [(_ "You're playing a text adventure game. You control the character by entering commands. Some available commands are:")
                                                        (_ "GO <direction>: move in the given compass direction. For example: \"GO NORTH\". \"NORTH\" and \"N\" will work too.")
                                                        (_ "TAKE <item>: add an item to your inventory.")
                                                        (_ "INVENTORY: list your inventory contents. \"I\" will work too.")
                                                        (_ "LOOK: look around.")
                                                        (_ "LOOK AT <item>: look at some specific item.")
                                                        (_ "LOOK IN <item>: look inside some container item.")
                                                        (_ "TALK TO <character>: start a conversation with another character.")
                                                        (_ "UNLOCK <item> WITH <item>: unlock some item using another one. For example: UNLOCK door WITH key.")
                                                        (_ "OPEN, CLOSE, READ, TURN ON, PUT IN, EAT, DRINK, KILL, etc. may work on some objects, just try.")
                                                        (_ "SAVE: save your current progress.")
                                                        (_ "RESTORE: restore a previously saved game.")
                                                        (_ "EXIT: close the game.")
                                                        (_ "You can use the TAB key to get completion suggestions for a command and the UP/DOWN arrows to search the command history.")])))

(def move (make-item-handler "move" :move))
(def pull (make-item-handler "pull" :pull))
(def push (make-item-handler "push" :push))
