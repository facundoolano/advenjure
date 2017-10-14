(ns advenjure.test-utils
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer [<!!]]
            [clojure.core.async.impl.protocols :refer [ReadPort]]
            [advenjure.verbs :as verbs]))

(def clean-str (comp clojure.string/capitalize clojure.string/trim))

(defn is-output
  "Compare the last n output lines with the given."
  [gs expected]
  (let [as-seq  (if (string? expected)
                  (list expected)
                  (seq expected))
        amount  (count as-seq)
        output  (clojure.string/split-lines (:out gs))
        results (take-last amount output)]
    (is (= (map clean-str as-seq) ;just ignore case man
           (map clean-str results)))
    gs))

(defn is-same [s1 s2]
  (is (= (dissoc s1 :out :__prevalue)
         (dissoc s2 :out :__prevalue))))

;; again we keep takin until it's out of the channel
(defn take-from-port
  [value]
  (if (satisfies? ReadPort value)
    (take-from-port (<!! value))
    value))

(defn get-verb
  [verb-name]
  (let [handler (->> verbs/verbs
                     (filter #(contains? (set (:commands %)) verb-name))
                     first
                     :handler)]
    (if (nil? handler) (println "AAAAAAAAA" verb-name)
        (fn [& args]
          (let [result (apply handler args)]
            (take-from-port result))))))
