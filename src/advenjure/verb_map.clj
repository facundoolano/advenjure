(ns advenjure.verb-map
  (:require [advenjure.map :refer [print-map]]
            [advenjure.verbs :refer :all]
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
                  (add-verb ["^go (?<dir>.*)" "^go$"] go)
                  (add-verb ["^look$" "^look around$" "^l$"] look)
                  (add-verb ["^map$" "^m$"] print-map)
                  (add-verb ["^look at (?<item1>.*)" "^look at$" "^describe (?<item2>.*)"
                             "^describe$"] look-at)
                  (add-verb ["^look in (?<item1>.*)" "^look in$" "^look inside (?<item2>.*)"
                             "^look inside$"] look-inside)
                  (add-verb ["^take (?<item1>.*)" "^take$" "^get (?<item2>.*)" "^get$"
                             "^pick (?<item1>.*)" "^pick$" "^pick up (?<item2>.*)"
                             "^pick (?<item>.*) up$" "^pick up$"] take_)
                  (add-verb ["^inventory$" "^i$"] inventory)
                  (add-verb ["^read (?<item>.*)" "^read$"] read_)
                  (add-verb ["^open (?<item>.*)" "^open$"] open)
                  (add-verb ["^close (?<item>.*)" "^close$"] close)
                  ; (add-verb ["^turn on (?<item>.*)" "^turn on$" "^turn (?<item>.*) on"] identity)
                  ; (add-verb ["^turn off (?<item>.*)" "^turn off$" "^turn (?<item>.*) off"] identity)
                  ; (add-verb ["^put (?<item>.*) in (?<item>.*)" "^put (?<item>.*) in$"
                  ;            "^put$" "^put (?<item>.*)$" "^put (?<item>.*) inside (?<item>.*)"
                  ;            "^put (?<item>.*) inside$"] identity)
                  (add-verb ["^unlock (?<item1>.*) with (?<item2>.*)" "^unlock (?<item>.*)"
                             "^unlock (?<item1>.*) with$" "^unlock$"
                             "^open (?<item1>.*) with (?<item2>.*)" "^open (?<item>.*) with"] unlock)
                  (add-verb ["^talk with (?<item>.*)" "^talk to (?<item>.*)" "^talk (?<item>.*)"]
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
