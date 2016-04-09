(ns advenjure.test-utils
  (:require [clojure.test :refer :all]))

;;;;; mock println
(def output (atom nil))

(defn say-mock
  "Save the speech lines to output, separated by '\n'"
  ([speech] (reset! output (clojure.string/join "\n" [@output speech])) nil))

(def clean-str (comp clojure.string/capitalize clojure.string/trim))

(defn is-output
  "Compare the last n output lines with the given."
  ([expected]
   (let [as-seq (if (string? expected)
                  (list expected)
                  (seq expected))
         lines (count as-seq)
         results (take-last lines (clojure.string/split-lines @output))]
     (is (= (map clean-str as-seq) ;just ignore case man
            (map clean-str results))))))

(use-fixtures :each (fn [f]
                      (reset! output nil)
                      (f)))
