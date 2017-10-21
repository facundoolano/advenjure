(ns advenjure.gettext.core
  (:require
   #?(:clj  [carica.core :as carica])
   #?(:cljs [goog.string :refer [format]])
   #?(:cljs [goog.string.format])))

(defmacro resolve-source
  "In order to make a config symbol available both in clj and cljs, wrap its
   evaluation in a macro, so it's done at compile time by clojure, when the
  config is available."
  []
  (let [sym    (carica/config :gettext-source)
        nspace (symbol (namespace sym))]
    (require nspace)
    sym))

(def text-source (resolve-source))

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
