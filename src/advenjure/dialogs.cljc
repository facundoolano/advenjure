(ns advenjure.dialogs
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go go-loop]]))
  (:require #?(:clj [clojure.core.async :refer [go go-loop <!]]
               :cljs [cljs.core.async :refer [<!]])
            #?(:cljs [goog.string :refer [format]])
            #?(:cljs [advenjure.eval :refer [eval]])
            [advenjure.async :as async]
            [advenjure.ui.input :as in]
            [advenjure.ui.output :as out]
            [advenjure.items :as items]))

(defn- format-line
  [character speech]
  (str character
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

  #?(:clj java.lang.String
     :cljs string)
  (run [dialog game-state]
    (out/print-line dialog)
    game-state)

  #?(:clj clojure.lang.IPersistentCollection
     :cljs cljs.core.PersistentVector)
  (run [dialog game-state]
    (reduce (fn [gs element] (run element gs))
            game-state
            (pack-strings dialog)))

  #?(:clj clojure.lang.Symbol
     :cljs cljs.core.Symbol)
  (run [dialog game-state]
    (run (eval dialog) game-state))

  #?(:clj clojure.lang.Fn
     :cljs function)
  (run [dialog game-state]
    (go (-> game-state async/force-chan <! dialog))))

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

;;; OPTIONAL DIALOGS

(defn- is-available
  [{executed :executed-dialogs :as game-state} {:keys [id sticky show-if]}]
  (and
   (or sticky (not (contains? executed id)))
   (or (not show-if) (show-if game-state))))

(defn- filter-available
  [game-state options]
  (vec (filter (partial is-available game-state) options)))

(defn- option-text
  [{:keys [dialog text]}]
  (or text (second dialog)))

(defn- print-options
  [options]
  (out/print-line)
  (doseq [[i opt] (map-indexed vector options)]
    (out/print-line (format "%d. %s" (inc i) (option-text opt))))
  (out/print-line))

(defn- select-option
  "Present the player with a list of options, read input and return the
  selected one. If only one option is available return that right away."
  [options]
  (let [amount  (count options)
        choices (set (range 1 (inc amount)))]
    (if (= amount 1)
      (go (first options))
      (do
        (print-options options)
        (go-loop [i (<! (in/read-value))]
          (if-not (contains? choices i)
            (recur (<! (in/read-value)))
            (get options (dec i))))))))

(defn- execute-optional
  [game-state options]
  (go-loop [available  (filter-available game-state options)
            game-state game-state]
    (let [{:keys [id dialog go-back]} (<! (select-option available))
          new-state                   (-> (run dialog game-state)
                                          async/force-chan
                                          <!
                                          (update :executed-dialogs conj id))
          remaining                   (filter-available new-state options)]
      (if (or go-back (empty? remaining))
        new-state
        (recur remaining new-state)))))

;; TODO allow to specify non map dialog when defaults
(defn optional
  [& options]
  (let [options (map #(assoc % :id (gensym "opt")) options)]
    (fn [game-state]
      (execute-optional game-state options))))

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
    (update game-state :events conj event-kw)))

(defn item?
  [item-name]
  (fn [game-state]
    (first (items/get-from (:inventory game-state) item-name))))

(defn not-item? [item-name]
  (comp not (item? item-name)))
