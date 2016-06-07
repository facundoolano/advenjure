(ns advenjure.verb-map
  (:require [advenjure.map :refer [print-map]]
            [advenjure.verbs :refer :all]
            [advenjure.utils :refer [direction-mappings]]
            [gettext.core :refer [_]]))

(defn add-verb
  "Adds the given function as the handler for every verb in the list."
  [verb-map verbs handler]
  (merge verb-map (zipmap verbs (repeat handler))))

(defn add-go-shortcuts
  "Allow commands like 'north' and 'n' instead of 'go north'"
  [vmap]
  (loop [new-map vmap
         [dir & remain] (keys direction-mappings)]
    (if (nil? dir)
      new-map
      (let [regexp (str "^" dir "$")]
        (recur (add-verb new-map [regexp] #(go % dir)) remain)))))

(def default-map (-> {}
                  add-go-shortcuts
                  (add-verb [(_ "^go (?<dir>.*)") (_ "^go$")] go)
                  (add-verb [(_ "^look$") (_ "^look around$") (_ "^l$")] look)
                  (add-verb [(_ "^map$") (_ "^m$")] print-map)
                  (add-verb [(_ "^look at (?<item1>.*)") (_ "^look at$") (_ "^describe (?<item2>.*)")
                             (_ "^describe$")] look-at)
                  (add-verb [(_ "^look in (?<item1>.*)") (_ "^look in$") (_ "^look inside (?<item2>.*)")
                             (_ "^look inside$")] look-inside)
                  (add-verb [(_ "^take (?<item1>.*)") (_ "^take$") (_ "^get (?<item2>.*)") (_ "^get$")
                             (_ "^pick (?<item1>.*)") (_ "^pick$") (_ "^pick up (?<item2>.*)")
                             (_ "^pick (?<item>.*) up$") (_ "^pick up$")] take_)
                  (add-verb [(_ "^inventory$") (_ "^i$")] inventory)
                  (add-verb [(_ "^read (?<item>.*)") (_ "^read$")] read_)
                  (add-verb [(_ "^open (?<item>.*)") (_ "^open$")] open)
                  (add-verb [(_ "^close (?<item>.*)") (_ "^close$")] close)
                  (add-verb [(_ "^unlock (?<item1>.*) with (?<item2>.*)") (_ "^unlock (?<item>.*)")
                             (_ "^unlock (?<item1>.*) with$") (_ "^unlock$")
                             (_ "^open (?<item1>.*) with (?<item2>.*)") (_ "^open (?<item>.*) with")] unlock)
                  (add-verb [(_ "^talk with (?<item>.*)") (_ "^talk to (?<item>.*)") (_ "^talk (?<item>.*)")]
                            talk)
                  (add-verb [(_ "^save$")] save)
                  (add-verb [(_ "^restore$") (_ "^load$")] restore)
                  (add-verb [(_ "^exit$")] exit)
                  (add-verb [(_ "^help$")] help)
                  (add-verb [(_ "^get up$") (_ "^stand up$") (_ "^stand$")] stand)))

;use a sorted version to extract the longest possible form first
(defn sort-verbs [verb-map] (reverse (sort-by count (keys verb-map))))
(def msort-verbs (memoize sort-verbs))

(defn match-verb [text verb]
  (let [[head & tokens :as full] (re-find (re-pattern verb) text)]
    (cond
      (and (not (nil? full)) (not (coll? full))) [verb '()] ;single match, no params
      (not-empty head) [verb tokens]))) ; match with params

(defn find-verb
  "Return [verb tokens] if there's a proper verb at the beginning of text."
  [verb-map text]
  (some (partial match-verb text) (msort-verbs verb-map)))
