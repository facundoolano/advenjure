(ns advenjure.verb-map
  (:require [advenjure.map :refer [print-map]]
            [advenjure.verbs :refer [go look look-at look-inside take_ inventory read_ open close unlock talk
                                     save restore exit help stand]]
            [advenjure.utils :refer [direction-mappings]]
            [advenjure.gettext.core :refer [_]]))

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
                  (add-verb [(_ "^go (.*)") (_ "^go$")] go)
                  (add-verb [(_ "^look$") (_ "^look around$") (_ "^l$")] look)
                  (add-verb [(_ "^map$") (_ "^m$")] print-map)
                  (add-verb [(_ "^look at (.*)") (_ "^look at$") (_ "^describe (.*)")
                             (_ "^describe$")] look-at)
                  (add-verb [(_ "^look in (.*)") (_ "^look in$") (_ "^look inside (.*)")
                             (_ "^look inside$")] look-inside)
                  (add-verb [(_ "^take (.*)") (_ "^take$") (_ "^get (.*)") (_ "^get$")
                             (_ "^pick (.*)") (_ "^pick$") (_ "^pick up (.*)")
                             (_ "^pick (.*) up$") (_ "^pick up$")] take_)
                  (add-verb [(_ "^inventory$") (_ "^i$")] inventory)
                  (add-verb [(_ "^read (.*)") (_ "^read$")] read_)
                  (add-verb [(_ "^open (.*)") (_ "^open$")] open)
                  (add-verb [(_ "^close (.*)") (_ "^close$")] close)
                  (add-verb [(_ "^unlock (.*) with (.*)") (_ "^unlock (.*)")
                             (_ "^unlock (.*) with$") (_ "^unlock$")
                             (_ "^open (.*) with (.*)") (_ "^open (.*) with")] unlock)
                  (add-verb [(_ "^talk with (.*)") (_ "^talk to (.*)") (_ "^talk (.*)")]
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
