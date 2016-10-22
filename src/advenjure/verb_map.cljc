(ns advenjure.verb-map
  (:require [advenjure.map :refer [print-map_]]
            [advenjure.verbs :refer [go go-back look look-at look-inside take_ inventory read_ open close unlock talk
                                     save restore exit help stand move push pull take-all use_]]
            [advenjure.utils :refer [direction-mappings]]
            #?(:cljs [xregexp])
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
                  (add-verb [(_ "^go (?<dir>.*)") (_ "^go$")] go)
                  (add-verb [(_ "^go back$") (_ "^back$") (_ "^b$")] go-back)
                  (add-verb [(_ "^look$") (_ "^look around$") (_ "^l$")] look)
                  (add-verb [(_ "^map$") (_ "^m$")] print-map_)
                  (add-verb [(_ "^look at (?<item1>.*)") (_ "^look at$") (_ "^describe (?<item2>.*)")
                             (_ "^describe$")] look-at)
                  (add-verb [(_ "^take all$") (_ "^take everything$")
                             (_ "^get all$") (_ "^get everything$")] take-all)
                  (add-verb [(_ "^look in (?<item1>.*)") (_ "^look in$") (_ "^look inside (?<item2>.*)")
                             (_ "^look inside$")] look-inside)
                  (add-verb [(_ "^take (?!all|everything$)(?<item>.*)") (_ "^take$") (_ "^get (?!all|everything$)(?<item>.*)") (_ "^get$")
                             (_ "^pick (?<item1>.*)") (_ "^pick$") (_ "^pick up (?<item2>.*)")
                             (_ "^pick (?<item>.*) up$") (_ "^pick up$")] take_)
                  (add-verb [(_ "^move (?<item>.*)") (_ "^move$")] move)
                  (add-verb [(_"^pull (?<item>.*)") (_"^pull$")] pull)
                  (add-verb [(_"^push (?<item>.*)") (_"^push$")] push)
                  (add-verb [(_ "^inventory$") (_ "^i$")] inventory)
                  (add-verb [(_ "^read (?<item>.*)") (_ "^read$")] read_)
                  (add-verb [(_ "^open (?<item>.*)") (_ "^open$")] open)
                  (add-verb [(_ "^close (?<item>.*)") (_ "^close$")] close)
                  (add-verb [(_ "^use (?<item>.*)") (_ "^use$")] use_)
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

(def regexp #?(:clj re-pattern :cljs js/XRegExp))

(defn match-verb [text verb]
  (let [[head & tokens :as full] (re-find (regexp verb) text)]
    (cond
      (and (not (nil? full)) (not (coll? full))) [verb '()] ;single match, no params
      (not-empty head) [verb tokens]))) ; match with params

(defn find-verb
  "Return [verb tokens] if there's a proper verb at the beginning of text."
  [verb-map text]
  (some (partial match-verb text) (msort-verbs verb-map)))
