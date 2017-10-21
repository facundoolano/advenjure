(ns advenjure.gettext.core
  #?(:cljs (:require-macros [advenjure.gettext.macro :refer [config]]))
  (:require
   [advenjure.text.en-past]
   #?(:clj [advenjure.gettext.macro :refer [config]])
   #?(:cljs [goog.string :refer [format]])
   #?(:cljs [goog.string.format])))

(if (config :gettext-source)
  (require [(symbol (namespace (symbol (config :gettext-source))))]))
(def ^:dynamic *text-source* (eval (symbol (config :gettext-source))))

;; copypasted from clojure-gettext for now, until I figure out how
;; to properly make it work for both clj and cljs

(defn gettext
  "Look up the given key in the current text source dictionary.
  If not found return the key itself."
  [text-key & replacements]
  (let [text-value (get *text-source* text-key text-key)
        text-value (if (fn? text-value) (text-value nil) text-value)]
    (apply format text-value replacements)))

(defn pgettext
  [ctx text-key & replacements]
  (let [text-value (get *text-source* text-key text-key)
        text-value (if (fn? text-value) (text-value ctx) text-value)]
    (apply format text-value replacements)))

; handy aliases
(def _ gettext)
(def p_ pgettext)
