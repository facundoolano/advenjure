(ns advenjure.ui.input
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.reader :refer [read-string register-tag-parser!]]
            [clojure.string :as string]
            [jquery]
            [jquery.terminal]
            [jquery.mousewheel]
            [advenjure.utils :refer [direction-mappings current-room]]
            [advenjure.items :refer [map->Item all-item-names]]
            [advenjure.rooms :refer [map->Room visible-name-mappings]]
            [cljs.core.async :refer [<! >! chan close!]]))

(def term #(.terminal (js/$ "#terminal")))

; put it inside an atom so it can be reset on figwheel restart
(def input-chan (atom (chan)))

(defn figwheel-cleanup
  "Need to recreate input chan so previous game loop doesnt receive input anymore."
  []
  (println "resetting input channel for figwheel cleanup")
  (close! @input-chan)
  (reset! input-chan (chan)))


(def exit #(.pause (term)))

(defn read-value []
  (let [key-chan (chan)]
    (go
      (.pause (term))
      (aset js/window "onkeydown"
        (fn [ev]
          (do
            (aset js/window "onkeydown" nil)
            (go (>! key-chan (aget ev "key")))
            (.resume (term)))))
      (read-string (<! key-chan)))))

(def read-key read-value)

; Needed to properly persist in cljs
(register-tag-parser! "advenjure.items.Item" map->Item)
(register-tag-parser! "advenjure.rooms.Room" map->Room)

(defn read-file [file]
  (read-string (aget js/localStorage file)))

(defn process-command
  "Write command to the input channel"
  [command term]
  (go (>! @input-chan command)))


(defn get-suggested-token
  "
  Compare the verb tokens with the complete input tokens and if they match,
  return the next verb token to be suggested. If no match returns nil.
  "
  [verb-tokens input-tokens]
  (loop [[verb & next-verbs] verb-tokens
         [input & next-inputs] input-tokens]
    (cond
      (nil? input) (str verb " ") ; all input matched, suggest current verb token
      (nil? verb) nil
      (= (string/trim input) (string/trim verb)) (recur next-verbs next-inputs)
      (string/starts-with? verb "(?<") (recur next-verbs next-inputs))))

(defn expand-suggestion
  [token items dirs]
  (cond
    (string/includes? token "?<item") (map #(str % " ") items)
    (string/includes? token "?<dir") (map #(str % " ") dirs)
    :else [token]))

(defn tokenize-verb
  [verb]
  (-> verb
      (string/replace #"\$" "")
      (string/replace #"\^" "")
      (string/split #" "))) ;; considers weird jquery &nbsp

(defn tokenize-input
  "
  Get the finished tokens (partial tokens are ingnored, since that part of the
  completion is handled by jquery terminal).
  Encodes/decodes item and dir names to avoid breaking them in separate tokens.
  "
  [input items dirs]
  (let [encode #(string/replace %1 (re-pattern %2) (string/replace %2 #" " "%%W%%"))
        input (string/replace input #"[\s|\u00A0]" " ")
        input (reduce encode input (concat items dirs))
        tokens (string/split input #" ")
        tokens (map #(string/replace %1 #"%%W%%" " ") tokens)]
    (if (= (last input) \space)
      tokens
      (butlast tokens))))

(defn get-full-input []
  (.text (.next (js/$ ".prompt"))))

(defn get-completion
  [game-state verb-map]
  (let [verb-tokens (map tokenize-verb (keys verb-map))
        room (current-room game-state)
        items (all-item-names (into (:inventory game-state) (:items room)))
        name-mappings (visible-name-mappings (:room-map game-state) (:current-room game-state))
        dirs (concat (keys direction-mappings) (keys name-mappings))]
    (fn [term input cb]
      (let [input (get-full-input)
            input-tokens (tokenize-input input items dirs)
            suggested1 (remove string/blank? (distinct (map #(get-suggested-token % input-tokens) verb-tokens)))
            suggested (distinct (mapcat #(expand-suggestion % items dirs) suggested1))]
        (cb (apply array suggested))))))

(defn set-interpreter
  [gs verb-map]
  (let [room (:name (current-room gs))
        moves (:moves gs)
        prompt (str "\n@" room " [" moves "] > ")]
    (if (> (.level (term)) 1) (.pop (term))) ; if there's a previous interpreter, pop it
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
    (<! @input-chan)))

(defn prompt-value
  "Ask the user to enter a value"
  [prompt]
  (go
    (.set_prompt (term) prompt)
    (<! @input-chan)))

