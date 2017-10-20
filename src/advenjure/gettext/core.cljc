(ns advenjure.gettext.core
  (:require
   [advenjure.text.en-past]
   #?(:cljs [goog.string :refer [format]])
   #?(:cljs [goog.string.format])))

(def text-source (atom advenjure.text.en-past/dictionary))

(defn set-dictionary!
  "Change the text source dictionary."
  [new-dictionary]
  (reset! text-source new-dictionary))

;; copypasted from clojure-gettext for now, until I figure out how
;; to properly make it work for both clj and cljs

(defn gettext
  "Look up the given key in the current text source dictionary.
  If not found return the key itself."
  [text-key & replacements]
  (let [text-value (get @text-source text-key text-key)
        text-value (if (fn? text-value) (text-value nil) text-value)]
    (apply format text-value replacements)))

(defn pgettext
  [ctx text-key & replacements]
  (let [text-value (get @text-source text-key text-key)
        text-value (if (fn? text-value) (text-value ctx) text-value)]
    (apply format text-value replacements)))

; handy aliases
(def _ gettext)
(def p_ pgettext)
