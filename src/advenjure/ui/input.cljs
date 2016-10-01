(ns advenjure.ui.input
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.reader :refer [read-string]]
            [clojure.string :as string]
            [advenjure.utils :refer [direction-mappings current-room]]
            [advenjure.items :refer [all-item-names]]
            [cljs.core.async :refer [<! >! chan]]))

(def term #(.terminal (.$ js/window "#terminal")))
(def input-chan (chan))

; FIXME on read value set a once listener, get value, unset
(defn read-value []
  (let [key-chan (chan)]
    (go
      (.pause (term))
      (aset js/window "onkeydown"
        (fn [ev]
          (do
            (aset js/window "onkeydown" nil)
            (println (aget ev "key"))
            (go (>! key-chan (aget ev "key")))
            (.resume (term)))))
      (read-string (<! key-chan)))))

; TODO
(def read-key read-value)

(defn load-file [file]
  (read-string (aget js/localStorage file)))


(defn process-command
  "Write command to the input channel"
  [command term]
  (go (>! input-chan command)))


(defn get-suggested-token
  "
  Compare the verb tokens with the complete input tokens and if they match,
  return the next verb token to be suggested. If no match returns nil.
  "
  [verb-tokens input-tokens]
  (loop [[verb & next-verbs] verb-tokens
         [input & next-inputs] input-tokens]
    (cond
      (nil? input) (str verb " ") ; all input matched, suggest current verb
      (= (string/trim input) (string/trim verb)) (recur next-verbs next-inputs)
      (string/starts-with? verb "(?<") (recur next-verbs next-inputs))))

(defn expand-suggestion
  [token items dirs]
  (cond
    (#{"(?<item>.*) " "(?<item1>.*) " "(?<item2>.*) "} token) items
    (= token "(?<dir>.*) ") dirs
    :else [token]))

(defn tokenize-verb
  [verb]
  (-> verb
      (string/replace #"\$" "")
      (string/replace #"\^" "")
      (string/split #" "))) ;; considers weird jquery &nbsp

(defn tokenize-input
  "
  get the finished tokens (partial tokens are ingnored, since that part of the
  completion is handled by jquery terminal).
  "
  [input]
  (let [input (string/replace input #"[\s|\u00A0]" " ")
        tokens (string/split input #" ")]
    (if (= (last input) \space)
      tokens
      (butlast tokens))))

(defn get-full-input []
  (.text (.next (.$ js/window ".prompt"))))

(defn get-completion
  [game-state verb-map]
  (let [verb-tokens (map tokenize-verb (keys verb-map))
        room (current-room game-state)
        items (all-item-names (into (:inventory game-state) (:items room)))
        dirs (keys direction-mappings)]
    (fn [term input cb]
      (let [input (get-full-input)
            input-tokens (tokenize-input input)
            suggested1 (distinct (map #(get-suggested-token % input-tokens) verb-tokens))
            suggested (remove string/blank? (mapcat #(expand-suggestion % items dirs) suggested1))]
        (println input-tokens)
        (println suggested1)
        (println suggested)
        (cb (apply array suggested))))))

; TODO pop if there's a prvevious interpreter
(defn set-interpreter
  [gs verb-map]
  (let [room (:name (current-room gs))
        moves (:moves gs)
        prompt (str "\n@" room " [" moves "] > ")]
    (.push (term)
           process-command
           (js-obj "prompt" prompt
                   "completion" (get-completion gs verb-map)))))

(defn get-input
  "Wait for input to be written in the input channel"
  [state verb-map]
  (go
    (set-interpreter state verb-map)
    (.echo (term) " ")
    (<! input-chan)))
