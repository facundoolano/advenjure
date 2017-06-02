(ns advenjure.ui.input
  (:require [clojure.string :as string]
            [clojure.core.async :refer [go <!]]
            [advenjure.items :refer [all-item-names]]
            [advenjure.rooms :refer [visible-name-mappings]]
            [advenjure.utils :refer [direction-mappings current-room room-as-item]])
  (:import [jline.console ConsoleReader]
           [jline.console.completer StringsCompleter ArgumentCompleter NullCompleter AggregateCompleter]))

(def console (ConsoleReader.))

(defn read-key []
  (go (str (char (.readCharacter console)))))

(def exit #(System/exit 0))

(defn read-value
  "read a single key and eval its value. Return nil if no value entered."
  []
  (go
    (let [input (<! (read-key))]
      (try
        (read-string input)
        (catch RuntimeException e nil)))))

(defn- verb-to-completer
  "Take a verb regexp and an available items completer, and return an
  ArgumentCompleter that respects the regexp."
  [verb items-completer dirs-completer]
  (let [verb   (string/replace (subs verb 1) #"\$" "")
        tokens (string/split verb #" ")
        mapper (fn [token] (cond
                             (string/includes? token "?<item") items-completer
                             (string/includes? token "?<dir")  dirs-completer
                             :else                             (StringsCompleter. [token])))]
    (ArgumentCompleter. (concat (map mapper tokens) [(NullCompleter.)]))))

(defn- update-completer
  [verbs items room-names]
  (let [current   (first (.getCompleters console))
        items     (StringsCompleter. items)
        dirs      (StringsCompleter. (concat (keys direction-mappings) room-names))
        arguments (map #(verb-to-completer % items dirs) verbs)
        aggregate (AggregateCompleter. arguments)]
    (.removeCompleter console current)
    (.addCompleter console aggregate)))

(defn- prompt [gs]
  (let [prompt-fn (get-in gs [:configuration :prompt])
        p         (prompt-fn gs)]
    (.readLine console p)))

(defn get-input
  ([game-state]
   (go
     (let [verb-map   (get-in game-state [:configuration :verb-map])
           verbs      (keys verb-map)
           room       (current-room game-state)
           room-names (keys (visible-name-mappings (:room-map game-state) (:current-room game-state)))
           all-items  (concat (:inventory game-state) (:items room) [(room-as-item room)])
           item-names (all-item-names all-items)]
       (update-completer verbs item-names room-names)
       (prompt game-state)))))

(defn read-file [file]
  (go (read-string (slurp file))))

(defn prompt-value
  "Ask the user to enter a value"
  [prompt]
  (go (.readLine console prompt)))
