(ns advenjure.dialogs
  (:require [advenjure.items :as items]))

(defn print-dialog
  [game-state character speech]
  (println (str character " —" speech))
  (read-line)
  game-state)

(defn eval-line
  "If line is a literal line, return the expression to print it.
  If it's a callable, return an expression that calls it."
  [line]
  (cond
    (and (seq? line) (= (first line) 'dialog)) (list line) ; case (dialog (dialog ...))
    (and (seq? line) (string? (eval (first line)))) `(print-dialog ~@line) ; case dialog literal
    :else (list line))) ; case function literal or symbol

(defmacro dialog
  "Expand a dialog definition into a function to execute it."
  [& lines]
  `(fn [game-state#]
     (-> game-state#
         ~@(map eval-line lines))))

;;; INLINE HELPERS
(defn event?
  [event-kw]
  (fn [game-state]
    (contains? (:events game-state) event-kw)))

(defn not-event? [event-kw]
  (comp not (event? event-kw)))

(defn set-event
  [event-kw]
  (fn [game-state]
    (update-in game-state [:events] conj event-kw)))

(defn item?
  [item-name]
  (fn [game-state]
    (items/get-from (:inventory game-state) item-name)))

(defn not-item? [item-name]
  (comp not (item? item-name)))

;;; OPTIONAL DIALOGS
(defn get-show-if
  "Check the list of option modifiers for :show-if and return the associated
  function if present."
  [modifiers]
  (loop [[mod & more] modifiers]
    (cond
      (= :show-if mod) (first more)
      (empty? more) `(fn [gs#] true)
      :else (recur more))))

(defn option-spec
  "Take an option expression and build a spec hash for it."
  [option]
  (let [[title dialog & modifiers] option
        modset (set modifiers)]
    (-> {:id (str (gensym "opt"))
         :title title
         :dialog dialog}
        (assoc :go-back (contains? modset :go-back))
        (assoc :sticky (contains? modset :sticky))
        (assoc :show-if (get-show-if modifiers)))))

(defn is-available
  [game-state option]
  (and
    (or (:sticky option) (not (contains? (:executed-dialogs game-state) (:id option))))
    ((eval (:show-if option)) game-state)))

(defn filter-available
  [game-state options]
  (vec (filter #(is-available game-state %) options)))

(defn print-options
  [options]
  (dorun ; TODO maybe use a more readable loop instead
    (map-indexed (fn [i opt]
                   (println (str (inc i) ". " (:title opt))))
                 options)))

(defn read-input
  "read a value from input. Return nil if no value entered."
  []
  (let [input (read-line)]
    (try
      (read-string input)
      (catch RuntimeException e nil))))

(defn select-option
  "Present the player with a list of options, read input and return the
  selected one. If only one option is available return that right away."
  [options]
  (let [amount (count options)
        choices (set (range 1 (inc amount)))]
    (if (= amount 1)
      (first options)
      (do
        (print-options options)
        (loop [i (read-input)] ;fails with empty string
          (if-not (contains? choices i)
            (recur (read-input))
            (get options (dec i))))))))

(defn execute-optional
  [game-state options]
  (loop [available (filter-available game-state options)
         game-state game-state]
    (let [option (select-option available)
          dialog-fn (eval (:dialog option))
          new-state (-> game-state
                        (dialog-fn)
                        (update-in [:executed-dialogs] conj (:id option)))
          remaining (filter-available new-state options)]

      (if (or (:go-back option) (empty? remaining))
        new-state
        (recur remaining new-state)))))

(defmacro optional
  "Present dialog options to the user and execute the one selected."
  [& options]
  (let [specs (map option-spec options)]
    `(fn [game-state#]
       (execute-optional game-state# (list ~@specs)))))

;;; OTHER DIALOG FORMS
(defmacro random
  "Given a list of dialog forms, return a function that would execute any of
  them randomly each time it's called."
  [& lines]
  (let [lines (vec (map (fn [l] `(dialog ~l)) lines))
        size (count lines)]
    `(fn [game-state#]
       (let [selected# (get ~lines (rand-int ~size))]
         (selected# game-state#)))))

(defmacro conditional
  "Return a function that will test the condition function using the game-state
  and execute the dialog line if true. If false and a second line is given,
  that will be executed instead."
  ([condition true-line]
   `(fn [game-state#]
      (if (~condition game-state#)
        ((dialog ~true-line) game-state#)
        game-state#)))
  ([condition true-line false-line]
   `(fn [game-state#]
      (if (~condition game-state#)
        ((dialog ~true-line) game-state#)
        ((dialog ~false-line) game-state#)))))
