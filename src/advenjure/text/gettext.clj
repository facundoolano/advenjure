(ns advenjure.text.gettext
  (:require [advenjure.text.en-past]))

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
  (let [text-value (get text-source text-key text-key)]
    (apply format text-value replacements)))


; handy alias
(def _ gettext)


(defn- get-files
  [dir]
  (filter #(not (.isDirectory %)) (file-seq (clojure.java.io/file dir))))

(defn- extract-text
  [expressions]
  (let [extract (fn [expr]
                  (if (and (seq? expr) (#{'_ 'gettext} (first expr)))
                    (second expr)))]
    (filter not-empty (map extract (tree-seq list? rest expressions)))))

(defn- zipzip [s] (zipmap s s))

(defn scan-files
  "Utility function. Walk the given directory and for every clj file extract
  the strings that appear enclosed by (_ )."
  [dir]
  (->>
    (get-files dir)
    (mapcat (comp extract-text read-string #(str "(" % ")") slurp))
    zipzip))


