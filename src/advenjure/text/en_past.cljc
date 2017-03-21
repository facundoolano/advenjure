(ns advenjure.text.en-past)

(defn starts-with-vowel
  [ctx]
  (let [vowel? (set "aeiouAEIOU")]
    (vowel? (ffirst (:names ctx)))))

; this is the default so we can just use the key everywhere
(def dictionary
  {"a %s" #(if (starts-with-vowel %) "an %s" "a %s")})
