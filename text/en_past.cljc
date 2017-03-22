(ns advenjure.text.en-past)

(defn starts-with-vowel
  [ctx]
  (let [vowel? (set "aeiouAEIOU")]
    (vowel? (ffirst (:names ctx)))))

(def dictionary
  {"a %s" #(if (starts-with-vowel %) "an %s" "a %s")
   "I wasn't carrying anything." "lalala"})
