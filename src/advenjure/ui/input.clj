(ns advenjure.ui.input
  (:require [clojure.string :as string]
            [advenjure.items :refer [all-item-names]]
            [advenjure.utils :refer [direction-mappings current-room]])
  (:import [jline.console ConsoleReader]
           [jline.console.completer StringsCompleter ArgumentCompleter NullCompleter AggregateCompleter]))

(def console (ConsoleReader.))

(defn read-key []
  (str (char (.readCharacter console))))

(def exit #(System/exit 0))

(defn read-value
  "read a single key and eval its value. Return nil if no value entered."
  []
  (let [input (read-key)]
    (try
      (read-string input)
      (catch RuntimeException e nil))))


(defn verb-to-completer
  "Take a verb regexp and an available items completer, and return an
  ArgumentCompleter that respects the regexp."
  [verb items-completer dirs-completer]
  (let [verb (string/replace (subs verb 1) #"\$" "")
        tokens (string/split verb #" ")
        mapper (fn [token] (cond
                             (#{"(?<item>.*)" "(?<item1>.*)" "(?<item2>.*)"} token) items-completer
                             (= token "(?<dir>.*)") dirs-completer
                             :else (StringsCompleter. [token])))]
    (ArgumentCompleter. (concat (map mapper tokens) [(NullCompleter.)]))))

(defn update-completer
  [verbs items]""
  (let [current (first (.getCompleters console))
        items (StringsCompleter. items)
        dirs (StringsCompleter. (keys direction-mappings))
        arguments (map #(verb-to-completer % items dirs) verbs)
        aggregate (AggregateCompleter. arguments)]
    (.removeCompleter console current)
    (.addCompleter console aggregate)))

(defn prompt [gs]
  (let [room (:name (current-room gs))
        moves (:moves gs)
        points (:points gs)
        p (str "\n@" room " [" moves "] > ")]
    (.readLine console p)))

(defn get-input
  ([game-state verb-map]
   (let [verbs (keys verb-map)
         room (current-room game-state)
         all-items (into (:inventory game-state) (:items room))
         item-names (all-item-names all-items)]
     (update-completer verbs item-names)
     (prompt game-state))))

(defn load-file [file] (read-string (slurp file)))
