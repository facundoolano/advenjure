(ns advenjure.hooks
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
   [advenjure.ui.input :as input]
   #?(:clj [clojure.core.async :refer [<! go]]
      :cljs [cljs.core.async :refer [<!]])
   #?(:cljs [advenjure.eval :refer [eval]])))

(defn execute
  "Pipe the game state through the hooks loaded for the given event kw,
  passing the extra parameters in each step."
  [game-state hook-kw & extra]
  (let [hooks      (get-in game-state [:configuration :hooks hook-kw])
        apply-hook (fn [gs hook] (or (apply hook (cons gs extra)) gs))]
    (reduce apply-hook game-state hooks)))

;; FIXME have a way to do this without prompt ?
(defn eval-precondition
  "If the condition is a function return it's value, otherwise return unchanged."
  [condition & args]
  (go
    (let [prompt    (:prompt condition)
          condition (eval (or (:pre condition) condition))
          args      (if prompt
                      (conj (vec args) (<! (input/prompt-value prompt)))
                      args)]
      (if (fn? condition)
        (apply condition args)
        condition))))

(defn eval-postcondition
  "If there's a postcondition defined, evaluate it and return new game-state.
  Otherwise return the game-state unchanged."
  [condition old-state new-state]
  (let [condition (eval (:post condition))
        new-state (or new-state old-state)]
    (if (fn? condition)
      (or (condition old-state new-state) new-state)
      new-state)))

(defn eval-direction
  "Eval the precondition found in the given direction of the current room."
  [game-state direction]
  (let [roomkw        (:current-room game-state)
        room          (get-in game-state [:room-map roomkw])
        dir-condition (direction room)]
    (eval-precondition dir-condition game-state)))
