(ns advenjure.verbs
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [clojure.set]
            [clojure.string :as str]
            #?(:clj [clojure.core.async :refer [<! go]]
               :cljs [cljs.core.async :refer [<!]])
            [advenjure.utils :refer [say say-inline find-item direction-mappings
                                     current-room remove-item replace-item capfirst
                                     directions direction-names get-visible-room]]
            [advenjure.change-rooms :refer [change-rooms]]
            [advenjure.hooks :refer [execute eval-precondition eval-postcondition
                                     eval-direction eval-direction-sync]]
            [advenjure.items :refer [print-list describe-container iname all-items]]
            [advenjure.rooms :as rooms]
            [advenjure.gettext.core :refer [_ p_]]
            [advenjure.ui.input :as input :refer [read-file]]
            [advenjure.ui.output :refer [write-file]]
            #?(:cljs [advenjure.eval :refer [eval]])))

;;;; FUNCTIONS TO BUILD VERBS

(defn- noop
  "Does nothing except optionally saying something, the item specific behavior
  is defined in the item spec."
  [kw]
  (fn [gs item & etc]
    (if-let [speech (get-in item [kw :say])]
      (say gs speech))))

(defn- ask-ambiguous
  [item-name items]
  (let [first-different (fn [spec] (first (filter #(not= % item-name) (:names spec))))
        names           (map first-different items)
        names           (map #(str "the " %) names)
        first-names     (clojure.string/join ", " (butlast names))]
    (str "Which " item-name "? "
         (capfirst first-names) " or " (last names) "?")))

;; TODO move to a separate ns
(defn- exclude-string
  [exclude]
  (str/re-quote-replacement
   (if (not-empty exclude)
     (format "(?!%s$)" (str/join "|" exclude))
     "")))

(defn- fill-implicit-item
  "convert `talk to` to `talk to $`"
  [command]
  (cond
    (not (str/includes? command "$"))        (str command " $")
    (and (str/includes? command "$1")
         (not (str/includes? command "$2"))) (str command " $2")
    (and (str/includes? command "$2")
         (not (str/includes? command "$1"))) (str command " $1")
    :else                                    command))

(defn- expand-missing-item
  "convert `talk to $` to [`talk to` `talk to $`]"
  [command]
  (cond
    (str/ends-with? command "$")  [command (str/replace command #" \$$" "")]
    (str/ends-with? command "$1") [command (str/replace command #" \$1$" "")]
    (str/ends-with? command "$2") [command (str/replace command #" \$2$" "")]
    :else                         [command]))

(defn- expand-item-placeholders
  "Translate user friendly placeholders to proper regex groups."
  [command exclude]
  (let [exclude (exclude-string exclude)]
    (-> command
        (str/replace #"\$1" (str exclude "(?<item1>.*)"))
        (str/replace #"\$2" (str exclude "(?<item2>.*)"))
        (str/replace #"\$" (str exclude "(?<item>.*)")))))

(defn- expand-item-commands
  "applies all the above to the command array"
  [commands exclude]
  (->> commands
       (map str/trim)
       (map fill-implicit-item)
       (mapcat expand-missing-item)
       (map #(expand-item-placeholders % exclude))))

(defn make-item-verb
  "Takes the verb name, the kw to look up at the item at the handler function,
  wraps the function with the common logic such as trying to find the item,
  executing pre/post conditions, etc."
  [{:keys [commands kw display kw-required handler ignore-item]
    :or   {kw-required true
           display     (first commands)
           handler     (noop kw)}
    :as   spec}]
  (assoc
   spec
   :commands (expand-item-commands commands ignore-item)
   :handler (fn
              ([game-state] (say game-state (_ "%s what?" display)))
              ([game-state item-name]
               (let [[item :as items] (find-item game-state item-name)
                     conditions       (kw item)]
                 (cond
                   (empty? items)      (say game-state (_ "I didn't see that."))
                   (> (count items) 1) (say game-state (ask-ambiguous item-name items))
                   :else
                   ;; TODO factor out the following?
                   (go (let [value (<! (eval-precondition conditions game-state))]
                         (cond
                           (string? value)                (say game-state value)
                           (false? value)                 (say game-state (_ "I couldn't %s that." display))
                           (and kw-required (nil? value)) (say game-state (_ "I couldn't %s that." display))
                           :else                          (let [before-state  (-> (assoc game-state :__prevalue value)
                                                                                  (execute :before-item-handler kw item))
                                                                handler-state (handler before-state item)
                                                                ;; TODO refactor this once before state is dropped
                                                                post-state    (eval-postcondition conditions before-state handler-state)]
                                                            (execute post-state :after-item-handler kw item)))))))))))

(defn make-compound-item-verb
  "The same as above but adapted to compund verbs."
  [{:keys [commands kw display kw-required handler ignore-item]
    :or   {kw-required true
           display     (first commands)
           handler     (noop kw)}
    :as   spec}]
  (assoc
   spec
   :commands (expand-item-commands commands ignore-item)
   :handler (fn
              ([game-state] (say game-state (_ "%s what?" display)))
              ([game-state item1] (say game-state (_ "%s %s with what?" display item1)))
              ([game-state item1-name item2-name]
               (let [[item1 :as items1] (find-item game-state item1-name)
                     [item2 :as items2] (find-item game-state item2-name)
                     conditions         (kw item1)]
                 (cond
                   (or (empty? items1) (empty? items2)) (say game-state (_ "I didn't see that."))
                   (> (count items1) 1)                 (say game-state (ask-ambiguous item1-name items1))
                   (> (count items2) 1)                 (say game-state (ask-ambiguous item2-name items2))
                   :else
                   (go (let [value (<! (eval-precondition conditions game-state item2))]
                         (cond
                           (string? value)                (say game-state value)
                           (false? value)                 (say game-state (_ "I couldn't %s that." display))
                           (and kw-required (nil? value)) (say game-state (_ "I couldn't %s that." display))
                           :else                          (let [before-state  (-> (assoc game-state :__prevalue value)
                                                                                  (execute :before-item-handler kw item1 item2))
                                                                handler-state (handler before-state item1 item2)
                                                                ;; TODO refactor this once old state is dropped
                                                                post-state (eval-postcondition conditions before-state handler-state)]
                                                            (execute post-state :after-item-handler kw item1 item2)))))))))))

(defn expand-direction-commands
  [commands exclude]
  (->> (expand-item-commands commands exclude)
       (map #(str/replace % #"<item" "<dir"))))

(defn make-direction-verb
  [{:keys [commands ignore-dir handler display]
    :or   {display (first commands)}
    :as   spec}]
  (assoc
   spec
   :commands (expand-direction-commands commands ignore-dir)
   :handler (fn
              ([game-state] (say game-state (_ "% where?" display)))
              ([game-state direction] (handler game-state direction)))))

(defn make-movement-item-verb
  "Constructs verbs that use items to change rooms."
  [{:keys [kw] :as spec}]
  (make-item-verb
   (assoc
    spec :handler
    (fn [game-state item]
      ;; hackishly getting __prevalue so we can get the room kw from the precondition
      (println "GOING TO " (:__prevalue game-state))
      (change-rooms game-state (:__prevalue game-state))))))

(defn make-say-verb
  [spec]
  (assoc spec :handler (fn [gs] (say gs (:say spec)))))

;;; VERB HANDLER DEFINITIONS
(defn- go-handler
  [game-state direction]
  (let [current (current-room game-state)]
    (if-let [dir (get direction-mappings direction)]
      (go
        (let [dir-value (<! (eval-direction game-state dir))]
          (cond
            (string? dir-value) (say game-state dir-value)
            (not dir-value)     (say game-state (or (:default-go current) (_ "Couldn't go in that direction.")))
            :else               (let [new-state (change-rooms game-state dir-value)]
                                  (eval-postcondition (dir current) game-state new-state)))))
      ;; it's not a direction name, maybe it's a room name
      (if-let [roomkw (get-visible-room game-state direction)]
        (change-rooms game-state roomkw)
        (say game-state "Go where?")))))

(defn- look-to-handler
  [game-state direction]
  (if-let [dir (get direction-mappings direction)]
    (let [dir-value (eval-direction-sync game-state dir)
          dir-room  (get-in game-state [:room-map dir-value])]
      (cond
        (string? dir-value)                        (say game-state (_ "That direction was blocked."))
        (not dir-value)                            (say game-state (_ "There was nothing in that direction."))
        (or (:known dir-room) (:visited dir-room)) (say game-state (_ "The %s was in that direction." (:name dir-room)))
        :else                                      (say game-state (_ "I didn't know what was in that direction."))))
    ;; it's not a direction name, maybe it's a room name
    (if-let [roomkw (get-visible-room game-state direction)]
      (let [room-name (get-in game-state [:room-map roomkw :name])
            ;; this feels kinda ugly:
            dir-kw    (roomkw (clojure.set/map-invert (current-room game-state)))
            dir-name  (dir-kw direction-names)]
        (say game-state (_ "The %s was toward %s." room-name dir-name)))
      (say game-state (_ "Look to where?")))))

(defn- go-back-handler
  [game-state]
  (if-let [roomkw (:previous-room game-state)]
    (if (rooms/connection-dir (current-room game-state) roomkw)
      (change-rooms game-state roomkw)
      (say game-state (_ "Where would back be?")))
    (say game-state (_ "Where would back be?"))))

(defn- look-handler
  [game-state]
  (as-> game-state game-state
    (say game-state (str (rooms/describe (current-room game-state))))
    (say game-state " ")
    (reduce (fn [gs dirkw]
              (let [dir-value (eval-direction-sync game-state dirkw)
                    dir-name  (dirkw direction-names)
                    dir-room  (get-in game-state [:room-map dir-value])]
                (if dir-value
                  (let [prefix (str dir-name ": ")]
                    (cond
                      (string? dir-value)                        (say gs (str prefix (_ "blocked.")))
                      (or (:visited dir-room) (:known dir-room)) (say gs (str prefix (:name dir-room) "."))
                      :else                                      (say gs (str prefix  "???"))))
                  gs)))
            game-state
            directions)))

(defn- inventory-handler
  [game-state]
  (if (empty? (:inventory game-state))
    (say game-state (_ "I wasn't carrying anything."))
    (say game-state (str (_ "I was carrying:") (print-list (:inventory game-state))))))

(defn- save-handler
  [game-state]
  (write-file "saved.game" (dissoc game-state :configuration))
  (say game-state (_ "Done.")))

(defn- restore-handler
  [game-state]
  (go
    (try
      (let [loaded-state (<! (read-file "saved.game"))
            saved-state  (assoc loaded-state :configuration (:configuration game-state))]
        (say saved-state (rooms/describe (current-room saved-state))))
      (catch #?(:clj java.io.FileNotFoundException :cljs js/Object) e (say game-state (_ "No saved game found."))))))

(defn- exit-handler
  [game-state]
  (say game-state (_ "Bye!"))
  (input/exit))

(defn- look-at-handler
  [game-state item]
  (say game-state (:description item)))

(defn- look-inside-handler
  [game-state item]
  (if-let [custom-say (get-in item [:look-in :say])]
    (say game-state custom-say)
    (if (:items item)
      (say game-state (describe-container item))
      (say game-state (_ "I couldn't look inside a %s." (iname item))))))

(defn- take-handler
  "Try to take an item from the current room or from a container object in the inventory."
  [game-state item]
  (if (contains? (:inventory game-state) item)
    (say game-state (_ "I already had that."))
    (-> game-state
        (say (get-in item [:take :say] (_ "Taken.")))
        (remove-item item)
        (update :inventory conj item))))

;; need to declare it first because take-all calls it
(def take_ (make-item-verb {:commands    [(_ "take") (_ "get") (_ "pick up") (_ "pick $ up")]
                            :ignore-item [(_ "all") (_ "everything")]
                            :help        (_ "Attempt to take the given item.")
                            :kw          :take
                            :handler     take-handler}))

(defn- take-all-handler
  "Go through every item in the room that defines a value for :take, and attempt
  to take it."
  [game-state]
  (let [items      (all-items (:items (current-room game-state)))
        takeable   (remove (comp nil? :take) items)
        item-names (map #(first (:names %)) takeable)
        do-take    (:handler take_)]
    (if (empty? item-names)
      (say game-state (_ "I saw nothing worth taking."))
      (reduce (fn [gs-chan iname]
                (go
                  (let [gs        (<! gs-chan)
                        gs        (say-inline gs (str iname ": "))
                        new-state (<! (do-take gs iname))]
                    (or new-state gs))))
              (go game-state)
              item-names))))

(defn- open-handler
  [game-state item]
  (cond
    (not (:closed item)) (say game-state (p_ item "It was already open."))
    (:locked item)       (say game-state (p_ item "It was locked."))
    :else                (let [open-item  (assoc item :closed false)
                               custom-say (get-in item [:open :say])
                               new-state  (cond
                                            custom-say         (say game-state custom-say)
                                            (:items open-item) (say game-state (describe-container open-item))
                                            :else              (say game-state (_ "Opened.")))]
                           (replace-item new-state item open-item))))

(defn- close-handler
  [game-state item]
  (if (:closed item)
    (say game-state (p_ item "It was already closed."))
    (let [closed-item (assoc item :closed true)]
      (-> game-state
          (say (get-in item [:close :say] (_ "Closed.")))
          (replace-item item closed-item)))))

(defn- unlock-handler
  [game-state locked key-item]
  (cond
    (not (:locked locked))            (say game-state (p_ locked "It wasn't locked."))
    (not= locked (:unlocks key-item)) (say game-state (_ "That didn't work."))
    :else                             (let [unlocked (assoc locked :locked false)]
                                        (-> game-state
                                            (say (get-in locked [:unlock :say] (_ "Unlocked.")))
                                            (replace-item locked unlocked)))))

(defn- open-with-handler
  [game-state closed key-item]
  (cond
    (not (:closed closed)) (say game-state (p_ closed "It was already open."))

    (or (and (:locked closed) (= closed (:unlocks key-item)))
        (= closed (:opens key-item)))
    (let [opened (merge closed {:locked false :closed false})]
      (-> game-state
          (say (get-in closed [:open-with :say] (_ "Opened.")))
          (replace-item closed opened)))

    :else (say game-state (_ "That didn't work."))))

(defn- talk-handler
  [game-state item]
  (let [dialog (eval (:dialog item))]
    (dialog game-state)))

(declare verbs)
;; FIXME rewrite to autogenerate from verb map help
;; (def help (make-say-verb (clojure.string/join "\n    " [(_ "You're playing a text adventure game. You control the character by entering commands. Some available commands are:")
;;                                                         (_ "GO <direction>: move in the given compass direction. For example: \"GO NORTH\". \"NORTH\" and \"N\" will work too.")
;;                                                         (_ "TAKE <item>: add an item to your inventory.")
;;                                                         (_ "INVENTORY: list your inventory contents. \"I\" will work too.")
;;                                                         (_ "LOOK: look around.")
;;                                                         (_ "LOOK AT <item>: look at some specific item.")
;;                                                         (_ "LOOK IN <item>: look inside some container item.")
;;                                                         (_ "TALK TO <character>: start a conversation with another character.")
;;                                                         (_ "UNLOCK <item> WITH <item>: unlock some item using another one. For example: UNLOCK door WITH key.")
;;                                                         (_ "OPEN, CLOSE, READ, TURN ON, PUT IN, EAT, DRINK, KILL, etc. may work on some objects, just try.")
;;                                                         (_ "SAVE: save your current progress.")
;;                                                         (_ "RESTORE: restore a previously saved game.")
;;                                                         (_ "EXIT: close the game.")
;;                                                         (_ "You can use the TAB key to get completion suggestions for a command and the UP/DOWN arrows to search the command history.")])))

;;;; VERB DEFINITIONS
(defn- make-go-shortcuts
  "Allow commands like 'north' and 'n' instead of 'go north'"
  []
  (map (fn [dir] {:commands [dir]
                  :handler  #(go-handler % dir)})
       (keys direction-mappings)))

(def verbs (concat (make-go-shortcuts)
                   [(make-direction-verb {:commands   [(_ "go") (_ "go to")]
                                          :help       (_ "Change the location according to the given direction.")
                                          :ignore-dir ["back"]
                                          :handler    go-handler})

                    (make-direction-verb {:commands [(_ "look to") (_ "look toward")]
                                          :help     (_ "Describe what's in the given direction.")
                                          :handler  look-to-handler})

                    {:commands [(_ "go back") (_ "back") (_ "b")]
                     :help     (_ "Go to the previous room, if possible.")
                     :handler  go-back-handler}

                    {:commands [(_ "look") (_ "look around") (_ "l")]
                     :help     (_ "Look around and enumerate available movement directions.")
                     :handler  look-handler}

                    {:commands [(_ "inventory") (_ "i")]
                     :help     (_ "Describe the inventory contents.")
                     :handler  inventory-handler}

                    {:commands [(_ "save")]
                     :help     (_ "Save the current game to a file.")
                     :handler  save-handler}

                    {:commands [(_ "restore") (_ "load")]
                     :help     (_ "Restore a previous game from file.")
                     :handler  restore-handler}

                    {:commands [(_ "exit")]
                     :help     (_ "Close the game.")
                     :handler  exit-handler}

                    (make-item-verb {:commands    [(_ "look at") (_ "describe")]
                                     :help        (_ "Look at a given item.")
                                     :kw          :look-at
                                     :kw-required false
                                     :handler     look-at-handler})

                    (make-item-verb {:commands    [(_ "look inside") (_ "look in")]
                                     :help        (_ "Look inside a given item.")
                                     :kw          :look-in
                                     :kw-required false
                                     :handler     look-inside-handler})

                    take_

                    {:commands [(_ "take all") (_ "take everything")
                                (_ "get all") (_ "get everything")]
                     :help     (_ "Attempt to take every visible object.")
                     :handler  take-all-handler}

                    (make-item-verb {:commands [(_ "open")]
                                     :help     (_ "Try to open a closed item.")
                                     :kw       :open
                                     :handler  open-handler})

                    (make-item-verb {:commands [(_ "close")]
                                     :help     (_ "Try to close an open item.")
                                     :kw       :close
                                     :handler  close-handler})

                    (make-compound-item-verb {:commands [(_ "unlock") (_ "unlock $1 with $2")]
                                              :display  (_ "unlock")
                                              :help     (_ "Try to unlock an item.")
                                              :kw       :unlock
                                              :handler  unlock-handler})

                    (make-compound-item-verb {:commands    [(_ "open $1 with $2")]
                                              :display     (_ "open")
                                              :help        (_ "Try to unlock and open a locked item.")
                                              :kw          :open-with
                                              :kw-required false
                                              :handler     open-with-handler})

                    (make-item-verb {:commands [(_ "talk to") (_ "talk with") (_ "talk")]
                                     :help     (_ "Talk to a given character or item.")
                                     :kw       :talk
                                     :handler  talk-handler})

                    ;; noop verbs
                    (make-item-verb {:commands [(_ "read")] :kw :read})
                    (make-item-verb {:commands [(_ "use")] :kw :use})
                    (make-compound-item-verb {:commands [(_ "use $1 with $2")]
                                              :kw       :use-with})
                    (make-item-verb {:commands [(_ "move")] :kw :move})
                    (make-item-verb {:commands [(_ "push")] :kw :push})
                    (make-item-verb {:commands [(_ "pull")] :kw :pull})

                    (make-say-verb {:commands [(_ "stand") (_ "stand up") (_ "get up")]
                                    :say      (_ "I was standing up already")})

                    (make-movement-item-verb {:commands [(_ "climb")] :kw :climb})
                    (make-movement-item-verb {:commands [(_ "climb down") (_ "climb $ down")]
                                              :kw       :climb-down})
                    (make-movement-item-verb {:commands [(_ "climb up") (_ "climb $ up")]
                                              :kw       :climb-up})
                    (make-movement-item-verb {:commands [(_ "enter")] :kw :enter})
                    ]))
