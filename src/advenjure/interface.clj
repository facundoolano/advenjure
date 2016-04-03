(ns advenjure.interface
  (:import [jline.console ConsoleReader]))

(defn clear-screen []
  (let [cr (ConsoleReader.)]
    (.clearScreen cr)))

(defn read-key []
  (let [cr (ConsoleReader.)]
    (str (char (.readCharacter cr)))))

(defn read-value
  "read a single key and eval its value. Return nil if no value entered."
  []
  (let [input (read-key)]
    (try
      (read-string input)
      (catch RuntimeException e nil))))

(def print-line println)

(defn term-size []
  (let [term (jline.TerminalFactory/get)
        w (.getWidth term)
        h (.getHeight term)]
    [w h]))

(defn get-input
  ([] (get-input "\n>"))
  ([prompt]
   (let [cr (ConsoleReader.)]
    (.readLine cr prompt))))
