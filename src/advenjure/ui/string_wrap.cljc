(ns advenjure.ui.string-wrap
  (:require [clojure.string :as string]))

(defn string-wrap
  ([text] (string-wrap text 80))
  ([text max-size]
   (loop [[word & others] (string/split text #" ")
          current ""
          lines []]
         (if word
             (let [new-current (str current " " word)
                               line-size (count (last (string/split new-current #"\n")))]
               (if (> max-size line-size)
                   (recur others new-current lines)
                 (recur others word (conj lines current))))
           (string/triml (string/join "\n" (conj lines current)))))))
