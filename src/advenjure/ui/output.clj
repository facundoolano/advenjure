(ns advenjure.ui.output
  (:import [jline.console ConsoleReader]))

(defn init []
  (.clearScreen (ConsoleReader.)))

(def print-line println)
