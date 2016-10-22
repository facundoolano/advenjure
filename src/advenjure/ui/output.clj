(ns advenjure.ui.output
  (:import [jline.console ConsoleReader]))

(defn init []
  (.clearScreen (ConsoleReader.)))

(defn clear []
  (.clearScreen (ConsoleReader.)))

(def print-line println)

(def write-file spit)
