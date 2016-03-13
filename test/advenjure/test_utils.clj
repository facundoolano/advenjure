(ns advenjure.test-utils
  (:require [clojure.test :refer :all]))

;;;;; mock println
(def output (atom nil))

(defn say-mock
  "Save the speech lines to output, separated by '\n'"
  ([speech] (reset! output (clojure.string/join "\n" [@output speech])) nil))

(defn is-output
  "Compare the last n output lines with the given."
  ([expected]
   (let [as-seq (if (string? expected)
                  (list expected)
                  (seq expected))
         lines (count as-seq)
         results (take-last lines (clojure.string/split-lines @output))]
     (is (= (map clojure.string/capitalize results) ;just ignore case man
            (map clojure.string/capitalize as-seq))))))

(use-fixtures :each (fn [f]
                      (reset! output nil)
                      (f)))