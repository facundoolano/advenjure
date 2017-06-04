(ns advenjure.dialogs
  (:require [advenjure.ui.input :as in]
            [advenjure.ui.output :as out]
            [advenjure.items :as items]))

(def pad-fmt (str "%-" (count "LONGESTNAME") "s"))

(defn- format-line
  [character speech]
  (str (format pad-fmt character)
       #?(:clj " —" :cljs " &mdash;")
       speech))

;; TODO make async and wait for read key
;; (defn- print-dialog
;;   [character speech]
;;   (out/print-line (format-line character speech))
;;   (in/read-key))

(defn- pack-strings
  "Loops through the sequence of lines, assuming that two sequential strings
  stand for character name and speech, and formats them accordingly."
  [lines]
  (loop [result                      []
         [character speech & remain] lines]
    (cond
      (not character)           result
      (not (string? character)) (recur (conj result character)
                                       (cons speech remain))
      (string? speech)          (recur (conj result (format-line character speech))
                                       remain)
      :else                     (recur (conj result character speech) remain))))

(defprotocol Dialog
  (run [dialog game-state]))

(extend-protocol Dialog

  java.lang.String
  (run [dialog game-state]
    (out/print-line dialog)
    game-state)

  clojure.lang.IPersistentCollection
  (run [dialog game-state]
    (reduce (fn [gs element] (run element gs))
            game-state
            (pack-strings dialog)))

  clojure.lang.Keyword
  (run [dialog game-state]
    (run (resolve dialog) game-state))

  clojure.lang.Fn
  (run [dialog game-state]
    (dialog game-state)))

(defn random
  [& lines]
  (fn [game-state]
    (run (rand-nth lines) game-state)))

(defn conditional
  ([condition true-d] (conditional true-d []))
  ([condition true-d false-d]
   (fn [game-state]
     (if (condition game-state)
       (run true-d game-state)
       (run false-d game-state)))))

;; TODO
(defn optional
  [& options])


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
    (first (items/get-from (:inventory game-state) item-name))))

(defn not-item? [item-name]
  (comp not (item? item-name)))
