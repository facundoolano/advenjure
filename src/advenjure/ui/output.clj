(ns advenjure.ui.output
  (:require [advenjure.ui.string-wrap :refer [string-wrap]])
  (:import [jline.console ConsoleReader]))

(defn init []
  (.clearScreen (ConsoleReader.)))

(defn clear []
  (.clearScreen (ConsoleReader.)))

(defn print-line [& strs]
  (println (map string-wrap strs)))

(def write-file spit)
