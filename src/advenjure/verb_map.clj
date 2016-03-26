(ns advenjure.verb-map
  (:require [advenjure.verbs :refer :all]
            [advenjure.utils :refer [direction-mappings]]))

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

(def verb-map (-> {}
                  add-go-shortcuts
                  (add-verb ["^go (.*)" "^talk (.*)" "^go$"] go)
                  (add-verb ["^look$" "^look around$" "^l$"] look)
                  (add-verb ["^look at (.*)" "^look at$" "^describe (.*)"
                             "^describe$"] look-at)
                  (add-verb ["^look in (.*)" "^look in$" "^look inside (.*)"
                             "^look inside$"] look-inside)
                  (add-verb ["^take (.*)" "^take$" "^get (.*)" "^get$"
                             "^pick (.*)" "^pick$" "^pick up (.*)"
                             "^pick (.*) up$" "^pick up$"] take_)
                  (add-verb ["^inventory$" "^i$"] inventory)
                  (add-verb ["^read (.*)" "^read$"] read_)
                  (add-verb ["^open (.*)" "^open$"] open)
                  (add-verb ["^close (.*)" "^close$"] close)
                  (add-verb ["^turn on (.*)" "^turn on$" "^turn (.*) on"]
                            identity)
                  (add-verb ["^turn off (.*)" "^turn off$" "^turn (.*) off"]
                            identity)
                  (add-verb ["^put (.*) in (.*)" "^put (.*) in$"
                             "^put$" "^put (.*)$" "^put (.*) inside (.*)"
                             "^put (.*) inside$"] identity)
                  (add-verb ["^unlock (.*) with (.*)" "^unlock (.*)"
                             "^unlock (.*) with$" "^unlock$"] unlock) ;FIXME open X with Y should work too
                  (add-verb ["^talk with (.*)" "^talk to (.*)" "^talk (.*)"]
                            talk)
                  (add-verb ["^save$"] save)
                  (add-verb ["^restore$" "^load$"] restore)
                  (add-verb ["^exit$"] exit)
                  (add-verb ["^help$"] help)
                  (add-verb ["^get up$" "^stand up$" "^stand$"] stand)))

;keep a sorted version to extract the longest possible form first
(def sorted-verbs (reverse (sort-by count (keys verb-map))))

(defn match-verb [text verb]
  (let [[head & tokens :as full] (re-find (re-pattern verb) text)]
    (cond
      (and (not (nil? full)) (not (coll? full))) [verb '()] ;single match, no params
      (not-empty head) [verb tokens]))) ; match with params

(defn find-verb
  "Return [verb tokens] if there's a proper verb at the beginning of text."
  [text]
  (some (partial match-verb text) sorted-verbs))