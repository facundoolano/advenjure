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
  (if (and (seq? line) (string? (eval (first line))))
    `(print-dialog ~@line)
    (list line)))

; (defmacro if-event
;   [event line]
;   `(fn [game-state#]
;      (if (contains? (:events game-state#) ~event)
;        (-> game-state#
;            ~(eval-line line)))))

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
    (not (contains? (:executed-dialogs game-state) (:id option)))
    ((eval (:show-if option)) game-state)))

(defn print-options
  [options]
  (dorun ; TODO maybe use a more readable loop instead
    (map-indexed (fn [i opt]
                   (println (str (inc i) ". " (:title opt))))
                 options)))

(defn read-option
  ; FIXME do some validation
  [available]
  (read-string (read-line)))

(defn execute-optional
  [game-state options]
  (let [available (into [] (filter #(is-available game-state %) options))]
    (print-options available)
    (let [i (read-option (count available))
          option (get available (dec i))
          dialog (eval (:dialog option))]
      (dialog game-state)))) ; SEEMS TO BE BREAKING HERE

(defmacro optional
  "Present dialog options to the user and execute the one selected."
  [& options]
  (let [specs (map option-spec options)]
    `(fn [game-state#]
       (execute-optional game-state# (list ~@specs)))))
