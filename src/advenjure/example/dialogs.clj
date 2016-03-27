(ns advenjure.example.dialogs
  (:require [advenjure.dialogs :refer :all]
            [advenjure.utils :as utils]))

(def PLAYER "ME")
(def NPC "NPC")

 ;;; By default a (dialog) entry would go sequentially through all of it's branches
 ;;; If a branch is a single line, i.e. (PLAYER "Hi"), just say it.
 ;;; If it's a special function (if-event, once, random) evaluate it according to its rules.
 ;;; If a branch is itself another a dialog, exhaust it first and then go back to the parent
 ;;; dialog and keep traversing.
 ;;; when using the jump function, the current tree traversal is dropped.

; the bool function will take game-state. If true at execution show the first
; dialog branch, if false the second (if any)
(def greet-npc (conditional (event? :knows-npc)
                            (PLAYER "Hi, NPC.")
                            (PLAYER "Hello!")))

; randomly select one of the given dialog lines
(def npc-says-hi (random (NPC "Hello.")
                         (NPC "Hi.")
                         (NPC "Hmmm.")))

; present the player the given dialog options. The cycle will be repeated while
; there are available options or until one with the :go-back modifier
; is executed. Options will be consumed after being executed, unless the
; :sticky modifier is used.
(def guess-npc
  (optional
    ("A not politically correct kind of person?"
        (dialog (PLAYER "A not politically correct kind of person?")
                (NPC "No.")))

    ("A non-deterministic polynomial-time complete?"
         (dialog (PLAYER "A non-deterministic polynomial-time complete?")
                 (NPC "No.")))

    ("A non-player character?"
         (dialog (PLAYER "A non-player character?")
                 (NPC "That's right.")
                 (set-event :knows-npc))
         :go-back)

    ("I give up." (dialog (PLAYER "I give up."))
        :sticky :go-back)))

(defn npc-moves
  "Magazine item is removed from inventory, NPC character is removed from room,
  player describes NPC leaving."
  [game-state]
  (let [magazine (utils/find-item game-state "magazine")
        npc (utils/find-item game-state "character")]
    (utils/say "And so the NPC took the magazine and left the room.")
    (-> game-state
        (utils/remove-item magazine)
        (utils/remove-item npc))))

(def npc-dialog-options
  (optional
    ("Who are you?"
          (dialog (PLAYER "Who are you?")
                  (NPC "I'm an NPC.")
                  guess-npc)
          :show-if (not-event? :knows-npc)
          :sticky)

    ("Why are you here?"
          (dialog (PLAYER "Why are you here?")
                  (NPC "The programmer put me here to test dialog trees.")))

    ("Would you move? I'm kind of in a hurry"
            (dialog (PLAYER "Would you move? I'm kind of in a hurry")
                    (NPC "Sorry, I can't let you pass until we exhaust our conversation.")
                    (set-event :knows-wont-move)))

    ("What do I have to do for you to move?"
           (dialog (PLAYER "What do I have to do for you to move?")
                   (NPC "You bring me something interesting to read."))
           :show-if (event? :knows-wont-move)
           :sticky)

    ("Do you want this magazine?"
         (dialog (PLAYER "Do you want this magazine?")
                 (NPC "I sure do, sir.")
                 npc-moves)
         :show-if (item? "magazine")
         :go-back)

    ("Bye." (dialog (PLAYER "Bye.")
                    (NPC "See you."))
            :sticky
            :go-back)))

(def npc-dialog
        (dialog greet-npc npc-says-hi npc-dialog-options))

