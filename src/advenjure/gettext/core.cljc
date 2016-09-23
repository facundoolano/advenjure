(ns advenjure.gettext.core
  #?(:cljs (:require [goog.string :as gstring]
                     [goog.string.format])
     :clj  (:require [gettext.core])))

; noop functions for now
#?(:cljs (def format gstring/format))

#?(:cljs (defn gettext [text-key & replacements]
            (apply format text-key replacements))
   :clj (def gettext gettext.core/gettext))

#?(:cljs (defn pgettext
          [ctx text-key & replacements]
          (apply format text-key replacements))
   :clj (def pgettext gettext.core/pgettext))

; handy aliases
(def _ gettext)
(def p_ pgettext)

