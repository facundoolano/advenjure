(ns advenjure.conditions
  #?(:cljs (:require [advenjure.eval :refer [eval]])))

(defn eval-precondition
  "If the condition is a function return it's value, otherwise return unchanged."
  [condition & args]
  (let [condition (eval (or (:pre condition) condition))]
    (if (fn? condition)
      (apply condition args)
      condition)))

(defn eval-postcondition
  "If there's a postcondition defined, evaluate it and return new game-state.
  Otherwise return the game-state unchanged."
  [condition old-state new-state]
  (let [condition (eval (:post condition))]
    (if (fn? condition)
      (or (condition old-state (or new-state old-state)) new-state)
      new-state)))
