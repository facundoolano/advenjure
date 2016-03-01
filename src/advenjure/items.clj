(ns advenjure.items
  (:require [clojure.string :as string]))


(defn iname [item] (first (:names item)))

(declare describe-container)

(defn print-list [items]
  (defn print-item [item]
    (def vowel? (set "aeiouAEIOU"))
    (str
      (if (vowel? (first (iname item))) "An " "A ")
      (iname item)))

  (string/join "\n"
               (for [item items]
                 (str (print-item item) (describe-container item ". ")))))

(defn describe-container
  ([container] (describe-container container ""))
  ([container prefix]
   (if-let [items (:items container)]
    (cond
      (:closed container) (str prefix "The " (iname container) " is closed.")
      (empty? items) (str prefix "The " (iname container) " is empty.")
      :else (str prefix "The " (iname container)
                 " contains:\n" (print-list items))))))
