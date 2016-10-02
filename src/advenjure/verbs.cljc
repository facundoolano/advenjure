(ns advenjure.verbs
  (:require [advenjure.utils :refer [say find-item direction-mappings current-room remove-item replace-item]]
            [advenjure.change-rooms :refer [change-rooms]]
            [advenjure.conditions :refer [eval-precondition eval-postcondition]]
            [advenjure.items :refer [print-list describe-container iname]]
            [advenjure.rooms :as rooms]
            [advenjure.gettext.core :refer [_ p_]]
            [advenjure.ui.input :as input :refer [load-file]]
            [advenjure.ui.output :refer [save-file]]
            #?(:cljs [advenjure.eval :refer [eval]])))

;;;; FUNCTIONS TO BUILD VERB HANDLERS
; there's some uglyness here, but it enables simple definitions for the verb handlers
(defn noop [& args])

(defn make-item-handler
  "Takes the verb name, the kw to look up at the item at the handler function,
  wraps the function with the common logic such as trying to find the item,
  executing pre/post conditions, etc."
  ; this one uses a noop handler, solely based on post/preconditions (i.e. read)
  ([verb-name verb-kw] (make-item-handler verb-name verb-kw noop))
  ([verb-name verb-kw handler &
    {:keys [kw-required] :or {kw-required true}}]
   (fn
     ([game-state] (say (_ "%s what?" verb-name)))
     ([game-state item-name]
      (let [items (find-item game-state item-name)
            item (first items)
            conditions (verb-kw item)
            value (eval-precondition conditions game-state)]
        (cond
          (empty? items) (say (_ "I didn't see that."))
          (> (count items) 1) (say (_ "Which %s?" item-name))
          (string? value) (say value)
          (false? value) (say (_ "I couldn't %s that." verb-name))
          (and kw-required (nil? value)) (say (_ "I couldn't %s that." verb-name))
          :else (let [new-state (handler game-state item)]
                  (eval-postcondition conditions game-state new-state))))))))

(defn make-compound-item-handler
  ; some copy pasta, but doesn't seem worth to refactor
  "The same as above but adapted to compund verbs."
  ([verb-name verb-kw] (make-item-handler verb-name verb-kw noop))
  ([verb-name verb-kw handler &
    {:keys [kw-required] :or {kw-required true}}]
   (fn
     ([game-state] (say (_ "%s what?" verb-name)))
     ([game-state item1] (say (_ "%s %s with what?" verb-name item1)))
     ([game-state item1-name item2-name]
      (let [items1 (find-item game-state item1-name)
            item1 (first items1)
            items2 (find-item game-state item2-name)
            item2 (first items2)
            conditions (verb-kw item1)
            value (eval-precondition conditions game-state item2)]
        (cond
          (or (empty? items1) (empty? items2)) (say (_ "I didn't see that."))
          (> (count items1) 1) (say (_ "Which %s?" item1-name))
          (> (count items2) 1) (say (_ "Which %s?" item2-name))
          (string? value) (say value)
          (false? value) (say (str "I couldn't " verb-name " that."))
          (and kw-required (nil? value)) (say (_ "I couldn't %s that." verb-name))
          :else (let [new-state (handler game-state item1 item2)]
                  (eval-postcondition conditions game-state new-state))))))))

;;; VERB HANDLER DEFINITIONS
(defn go
  "Change the location if direction is valid"
  ([game-state] (say (_ "Go where?")))
  ([game-state direction]
   (if-let [dir (get direction-mappings direction)]
     (let [dir-condition (dir (current-room game-state))
           dir-value (eval-precondition dir-condition game-state)]
       (cond
         (string? dir-value) (say dir-value)
         (not dir-value) (say (_ "Couldn't go in that direction"))
         :else (let [new-state (change-rooms game-state dir-value)]
                 (eval-postcondition dir-condition game-state new-state))))

     (say "Go where?"))))

(defn look
  "Look around (describe room). If tokens is defined, show error phrase."
  [game-state]
  (say (rooms/describe (current-room game-state))))

(defn inventory
  "Describe the inventory contents."
  [game-state]
  (if (empty? (:inventory game-state))
    (say (_ "I wasn't carrying anything."))
    (say (str (_ "I was carrying:") (print-list (:inventory game-state))))))

(defn save
  "Save the current game state to a file."
  [game-state]
  (save-file "saved.game" game-state)
  (say (_ "Done.")))

(defn restore
  "Restore a previous game state from file."
  [game-state]
  (try
    (let [loaded-state (load-file "saved.game")]
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
     (if (:items item)
       (say (describe-container item))
       (say (_ "I couldn't look inside a %s." (iname item)))))
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
         (say (_ "Taken."))
         (assoc new-state :inventory new-inventory))))))

(def open
  (make-item-handler
   (_ "open") :open
   (fn [game-state item]
     (cond
       (not (:closed item)) (say (p_ item "It was already open."))
       (:locked item) (say (p_ item "It was locked."))
       :else (let [open-item (assoc item :closed false)]
               (if (:items open-item)
                (say (describe-container open-item))
                (say (_ "Opened.")))
               (replace-item game-state item open-item))))))

(def close
  (make-item-handler
   (_ "close") :close
   (fn [game-state item]
     (if (:closed item)
       (say (p_ item "It was already closed."))
       (let [closed-item (assoc item :closed true)]
         (say (_ "Closed."))
         (replace-item game-state item closed-item))))))

(def unlock
  (make-compound-item-handler
   (_ "unlock") :unlock
   (fn [game-state locked key-item]
     (cond
       (not (:locked locked)) (say (p_ locked "It wasn't locked."))
       (not= locked (:unlocks key-item)) (say (_ "That didn't work."))
       :else (let [unlocked (assoc locked :locked false)]
               (say (_ "Unlocked."))
               (-> game-state
                   (remove-item key-item)
                   (replace-item locked unlocked)))))))

(def talk
  (make-item-handler
   (_ "talk to") :talk
   (fn [game-state item]
     (let [dialog (eval (:dialog item))]
       (dialog game-state)))))

;;; NOOP VERBS (rely entirely in pre/post conditions)
(def read_ (make-item-handler (_ "read") :read))

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
