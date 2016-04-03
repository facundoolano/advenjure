(ns advenjure.dialogs-test
  (:require [clojure.test :refer :all]
            [advenjure.test-utils :refer :all]
            [advenjure.interface :refer :all]
            [advenjure.dialogs :refer :all]
            [advenjure.rooms :as room]
            [advenjure.items :as it]
            [advenjure.verbs :refer [talk]]))

(def simple (dialog ("ME" "Hi!")
                    ("YOU" "Hey there!")))

(def compound (dialog ("ME" "Hi!")
                      ("YOU" "Hey there!")
                      (dialog ("ME" "Bye then"))))

(def referenced (dialog simple
                        ("ME" "Bye then")))

(def bedroom (room/make "Bedroom" "short description of bedroom"))

(def game-state {:inventory #{(it/make "sword")}
                 :events #{:had-breakfast}
                 :executed-dialogs #{}
                 :current-room :bedroom
                 :room-map {:bedroom bedroom}})

(def cond-event (conditional (not-event? :had-breakfast)
                             ("ME" "I'm hungry.")
                             ("ME" "I'm full.")))

(def cond-item (conditional (item? "sword")
                            ("ME" "I have a shiny sword.")
                            ("ME" "I have nothin'")))

(def choice
  (optional
    ("What's your name?"
        (dialog ("ME" "What's your name?")
                ("YOU" "Emmett Brown.")))

    ("Where are you from?"
        (dialog ("ME" "Where are you from?")
                ("YOU" "Hill Valley."))
        :go-back)))


(deftest basic-dialogs
  (with-redefs [print-line say-mock
                read-key (fn [] nil)]
    (testing "linear dialog"
      (let [character (it/make ["character"] "" :dialog `simple)
            new-state (assoc-in game-state
                                [:room-map :bedroom :items] #{character})]
        (talk new-state "character")
        (is-output ["ME —Hi!" "YOU —Hey there!"])))


    (testing "compound literal dialog"
      (let [character (it/make ["character"] "" :dialog `compound)
            new-state (assoc-in game-state
                                [:room-map :bedroom :items] #{character})]
        (talk new-state "character")
        (is-output ["ME —Hi!" "YOU —Hey there!" "ME —Bye then"])))


    (testing "compound referenced dialog"
      (let [character (it/make ["character"] "" :dialog `referenced)
            new-state (assoc-in game-state
                                [:room-map :bedroom :items] #{character})]
        (talk new-state "character")
        (is-output ["ME —Hi!" "YOU —Hey there!" "ME —Bye then"])))

    (testing "conditional event"
      (let [character (it/make ["character"] "" :dialog `cond-event)
            new-state (assoc-in game-state
                                [:room-map :bedroom :items] #{character})]
        (talk new-state "character")
        (is-output "ME —I'm full.")))

    (testing "conditional item"
      (let [character (it/make ["character"] "" :dialog `cond-item)
            new-state (assoc-in game-state
                                [:room-map :bedroom :items] #{character})]
        (talk new-state "character")
        (is-output "ME —I have a shiny sword.")))))


(deftest optional-dialogs
  (with-redefs [print-line say-mock
                read-key (fn [] "1")]
    (testing "simple choice and go back"
      (let [character (it/make ["character"] "" :dialog `choice)
            new-state (assoc-in game-state
                                [:room-map :bedroom :items] #{character})]
        (talk new-state "character")
        (is-output ["1. What's your name?"
                    "2. Where are you from?"
                    "ME —What's your name?"
                    "YOU —Emmett Brown."
                    "ME —Where are you from?" ; only one option, autoselects
                    "YOU —Hill Valley."])))))
