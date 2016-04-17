(ns advenjure.text.gettext
  (:require [advenjure.text.en-past]
            [clojure.test :refer [function?]]))

; Load text source from project.clj
(def project-map (->> "project.clj" slurp read-string (drop 3) (partition 2) (map vec) (into {})))
(def source-ns (first (clojure.string/split (str (:gettext-source project-map)) #"/")))
(require `[~(symbol source-ns)])
(def ^:dynamic *text-source* (eval (:gettext-source project-map)))

(defn gettext
  "Look up the given key in the current text source dictionary.
  If not found return the key itself."
  [text-key & replacements]
  (let [text-value (get *text-source* text-key text-key)
        text-value (if (function? text-value) (text-value nil) text-value)]
    (apply format text-value replacements)))

(defn pgettext
  [ctx text-key & replacements]
  (let [text-value (get *text-source* text-key text-key)
        text-value (if (function? text-value) (text-value ctx) text-value)]
    (apply format text-value replacements)))

; handy alias
(def _ gettext)
(def p_ pgettext)

(defn- get-files
  [dir]
  (remove #(.isDirectory %) (file-seq (clojure.java.io/file dir))))

(defn- extract-text
  [expressions]
  (let [extract (fn [expr]
                  (cond
                    (and (seq? expr) (#{'_ 'gettext} (first expr))) (second expr)
                    (and (seq? expr) (#{'p_ 'pgettext} (first expr))) (nth expr 2)))]
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


