
 (def PLAYER "PLAYER")
 (def NPC "NPC")

 ;;; By default a (dialog) entry would go sequentially through all of it's branches
 ;;; If a branch is a single line, i.e. (PLAYER "Hi"), just say it.
 ;;; If it's a special function (if-event, once, random) evaluate it according to its rules.
 ;;; If a branch is itself another a dialog, exhaust it first and then go back to the parent
 ;;; dialog and keep traversing.
 ;;; when using the jump function, the current tree traversal is dropped.

; will use the first line/dialog that "evaluates to available"
(conditional greet-npc
             (once (PLAYER "Hello!")) ; will use it first time then kill it
             (if-event :knows-npc (PLAYER "Hi, NPC."))
             (PLAYER "Hi again.")) ; won't kill it b/c it's a regular single line

; use any of the given lines
(random npc-says-hi
        (NPC "Hello.")
        (NPC "Hi.")
        (NPC "Hmmm."))

; present the player the given dialog options. This will only be exhausted while there are
; selectable options (i.e options not consumed by "once" or discarded by event conditions)
; a jump can be used to force the exit of the options cycle.
(optional guess-npc
          (once (PLAYER "A not politically correct kind of person?")
                (NPC "No."))

          (once (PLAYER "A non-deterministic polynomial-time complete?")
                (NPC "No."))

          (dialog (PLAYER "A non-player character?")
                  (NPC "That's right.")
                  (set-event :knows-npc)
                  (jump npc-dialog-options))

          (dialog (PLAYER "I give up")
                  (jump npc-dialog-options)))

(dialog npc-dialog-start
        greet-npc
        npc-says-hi
        npc-dialog-options)

(optional npc-dialog-options
          (if-not-event :knows-npc (PLAYER "Who are you?")
                                   (NPC "I'm an NPC.")
                                   guess-npc)

          (once (PLAYER "Why are you here?")
                (NPC "The programmer put me here to test dialog trees."))

          (once (PLAYER "Would you move? I'm kind of in a hurry")
                (NPC "Sorry, I can't let you pass until we exhaust our conversation.")
                (set-event :knows-wont-move))

          (if-event :knows-wont-move
                    (PLAYER "What do I have to do for you to move?")
                    (NPC "You bring me something interesting to read."))

          (if-item magazine
                   (PLAYER "Do you want this magazine?")
                   (NPC "I sure do, sir.")
                   (change-game npc-moves)
                   (end-dialog))

          (dialog (PLAYER "Bye.")
                  (NPC "See you.")
                  (end-dialog)))

(defn npc-moves
  "Magazine item is removed from inventory, NPC character is removed from room,
  player describes NPC leaving."
  [game-state])
