(ns advenjure.dialogs-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer [go <!!]]
            [advenjure.test-utils :refer :all]
            [advenjure.ui.output :refer :all]
            [advenjure.ui.input :refer :all]
            [advenjure.dialogs :as dialogs]
            [advenjure.rooms :as room]
            [advenjure.items :as it]))


(def talk (get-verb "talk"))

(def simple ["ME" "Hi!"
             "YOU" "Hey there!"])

(def compound ["ME" "Hi!"
               "YOU" "Hey there!"
               ["ME" "Bye then"]])

(def referenced [simple
                 "ME" "Bye then"])

(def bedroom (room/make "Bedroom" "short description of bedroom"))

(def game-state {:inventory #{(it/make "sword")}
                 :events #{:had-breakfast}
                 :executed-dialogs #{}
                 :current-room :bedroom
                 :room-map {:bedroom bedroom}})

(def cond-event (dialogs/conditional (dialogs/not-event? :had-breakfast)
                                     ["ME" "I'm hungry."]
                                     ["ME" "I'm full."]))

(def cond-item (dialogs/conditional (dialogs/item? "sword")
                                    ["ME" "I have a shiny sword."]
                                    ["ME" "I have nothin'"]))

(def choice
  (dialogs/optional
   ;; allow both single dialog or map
   ["ME" "What's your name?"
    "YOU" "Emmett Brown."]
   {:dialog  ["ME" "Where are you from?"
              "YOU" "Hill Valley."]
    :go-back true}))

;; keep the output in an atom like a map as expected by is-output
(def output (atom {:out ""}))

(defn print-mock
  "Save the speech lines to output, separated by '\n'"
  ([& speech]
   (swap! output update :out str (apply str speech) "\n") nil))

(deftest basic-dialogs
  (with-redefs [print-line print-mock
                read-key   (fn [] (go nil))]
    (testing "linear dialog"
      (let [character (it/make ["character"] "you" :dialog `simple)
            new-state (assoc-in game-state
                                [:room-map :bedroom :items] #{character})]
        (talk new-state "character")
        (is-output @output ["ME —Hi!" "YOU —Hey there!"])))

    (testing "compound literal dialog"
      (let [character (it/make ["character"] "you" :dialog `compound)
            new-state (assoc-in game-state
                                [:room-map :bedroom :items] #{character})]
        (talk new-state "character")
        (is-output @output ["ME —Hi!" "YOU —Hey there!" "ME —Bye then"])))


    (testing "compound referenced dialog"
      (let [character (it/make ["character"] "you" :dialog `referenced)
            new-state (assoc-in game-state
                                [:room-map :bedroom :items] #{character})]
        (talk new-state "character")
        (is-output @output ["ME —Hi!" "YOU —Hey there!" "ME —Bye then"])))

    (testing "conditional event"
      (let [character (it/make ["character"] "you" :dialog `cond-event)
            new-state (assoc-in game-state
                                [:room-map :bedroom :items] #{character})]
        (talk new-state "character")
        (is-output @output "ME —I'm full.")))

    (testing "conditional item"
      (let [character (it/make ["character"] "you" :dialog `cond-item)
            new-state (assoc-in game-state
                                [:room-map :bedroom :items] #{character})]
        (talk new-state "character")
        (is-output @output "ME —I have a shiny sword.")))))

(deftest optional-dialogs
  (with-redefs [print-line print-mock
                read-key   (fn [] (go "1"))]
    (testing "simple choice and go back"
      (let [character (it/make ["character"] "you" :dialog `choice)
            new-state (assoc-in game-state
                                [:room-map :bedroom :items] #{character})]
        (talk new-state "character")
        (is-output @output ["1. What's your name?"
                            "2. Where are you from?"
                            ""
                            "ME —What's your name?"
                            "YOU —Emmett Brown."
                            "ME —Where are you from?" ; only one option, autoselects
                            "YOU —Hill Valley."])))))
