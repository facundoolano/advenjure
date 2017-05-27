(ns advenjure.test-utils
  (:require [clojure.test :refer :all]))

(def clean-str (comp clojure.string/capitalize clojure.string/trim))

(defn is-output
  "Compare the last n output lines with the given."
  [gs expected]
  (let [as-seq (if (string? expected)
                 (list expected)
                 (seq expected))
        amount (count as-seq)
        output (clojure.string/split-lines (:out gs))
        results (take-last amount output)]
    (is (= (map clean-str as-seq) ;just ignore case man
           (map clean-str results)))
    gs))
