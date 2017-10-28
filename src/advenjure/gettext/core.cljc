(ns advenjure.gettext.core
  (:require
   [advenjure.text.en-past]
   #?(:cljs [goog.string :refer [format]])
   #?(:cljs [goog.string.format])))

;; TODO the text source is hardcoded since we still need to figure out a way to
;; statically configure dictionary in clojurescript
(def text-source advenjure.text.en-past/dictionary)

(defn gettext
  "Look up the given key in the current text source dictionary.
  If not found return the key itself."
  [text-key & replacements]
  (let [text-value (get text-source text-key text-key)
        text-value (if (fn? text-value) (text-value nil) text-value)]
    (apply format text-value replacements)))

(defn pgettext
  [ctx text-key & replacements]
  (let [text-value (get text-source text-key text-key)
        text-value (if (fn? text-value) (text-value ctx) text-value)]
    (apply format text-value replacements)))

; handy aliases
(def _ gettext)
(def p_ pgettext)
