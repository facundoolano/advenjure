(ns advenjure.items)


(defn iname [item] (first (:names item)))

(declare describe-container)

(defn print-list [items]
  (defn print-item [item]
    (def vowel? (set "aeiouAEIOU"))
    (str
      (if (vowel? (first (iname item))) "An " "A ")
      (iname item)))

  (clojure.string/join "\n"
                       (for [item items]
                         (str (print-item item) (describe-container item)))))

(defn describe-container [container]
  (if-let [items (:items container)]
    (cond
      (:closed container) (str "The " (iname container) " is closed.")
      (empty? items) (str "The " (iname container) " is empty.")
      :else (str "The " (iname container)
                 " contains:\n" (print-list items)))))
