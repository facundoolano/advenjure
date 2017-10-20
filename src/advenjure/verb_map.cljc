(ns advenjure.verb-map
  (:require [advenjure.verbs :as verbs]
            #?(:cljs [xregexp])))

(defn expand-verb
  "Creates a map of command regexes to handler for the given verb spec."
  [{:keys [commands handler]}]
  (let [patterns (map #(str "^" % "$") commands)]
    (zipmap patterns (repeat handler))))

(defn make-default-map
  []
  (apply merge (map expand-verb (verbs/make-default-verbs))))

;use a sorted version to extract the longest possible form first
(defn sort-verbs [verb-map] (reverse (sort-by count (keys verb-map))))
(def msort-verbs (memoize sort-verbs))

(def regexp #?(:clj re-pattern :cljs js/XRegExp))

(defn match-verb [text verb]
  (let [[head & tokens :as full] (re-find (regexp verb) text)]
    (cond
      (and (not (nil? full)) (not (coll? full))) [verb '()] ;single match, no params
      (not-empty head)                           [verb tokens]))) ; match with params

(defn find-verb
  "Return [verb tokens] if there's a proper verb at the beginning of text."
  [verb-map text]
  (some (partial match-verb text) (msort-verbs verb-map)))
