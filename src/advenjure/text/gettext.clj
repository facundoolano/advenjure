(ns advenjure.text.gettext
  (:require [advenjure.text.en-past]
            [clojure.test :refer [function?]]))

; Ok, not immutable, but I can't pass it around as an argument everywhere.
; maybe some config propertie, but don't want to tie it to lein
(def text-source (atom advenjure.text.en-past/dictionary))

(defn settext
  "Set the current text source to the given dictionary."
  [dictionary]
  (reset! text-source dictionary))

(defn gettext
  "Look up the given key in the current text source dictionary.
  If not found return the key itself."
  [text-key & replacements]
  (let [text-value (get @text-source text-key text-key)
        text-value (if (function? text-value) (text-value nil) text-value)]
    (apply format text-value replacements)))

(defn pgettext
  [ctx text-key & replacements]
  (let [text-value (get @text-source text-key text-key)
        text-value (if (function? text-value) (text-value ctx) text-value)]
    (apply format text-value replacements)))

; handy alias
(def _ gettext)
(def p_ pgettext)

(defn- get-files
  [dir]
  (filter #(not (.isDirectory %)) (file-seq (clojure.java.io/file dir))))

; fixme use pgettext
; fixme won't consider entries in a vector
(defn- extract-text
  [expressions]
  (let [extract (fn [expr]
                  (cond
                    (and (seq? expr) (#{'_ 'gettext} (first expr))) (second expr)
                    (and (seq? expr) (#{'p_ 'pgettext} (first expr))) (second (rest expr))))]
    (filter not-empty (map extract (tree-seq coll? identity expressions)))))

(defn- zipzip [s] (zipmap s s))

(defn scan-files
  "Utility function. Walk the given directory and for every clj file extract
  the strings that appear enclosed by (_ )."
  [dir]
  (->>
    (get-files dir)
    (mapcat (comp extract-text read-string #(str "(" % ")") slurp))
    zipzip
    (into (sorted-map))))


