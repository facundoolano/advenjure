(ns advenjure.game-test
  (:require [clojure.test :refer :all]
            [advenjure.test-utils :refer :all]
            [advenjure.utils :refer [current-room]]
            [advenjure.game :refer :all]
            [advenjure.rooms :as room]
            [advenjure.items :as it]))

;;;;;; some test data
(def drawer (it/make ["drawer"] "it's an open drawer." :closed false
                     :items #{(it/make "pencil" "it's a pencil" :take true)}))
(def sock (it/make ["sock"] "a sock" :take true))
(def magazine (it/make ["magazine" "sports magazine"]
                       "The cover reads 'Sports Almanac 1950-2000'"
                       :take true
                       :read "Tells the results of every major sports event till the end of the century."))
(def bedroom (room/make "Bedroom" "short description of bedroom"
                        :initial-description "long description of bedroom"
                        :items #{(it/make ["bed"] "just a bed") drawer sock}
                        :north :living
                        :visited true))

(def living (room/make "Bedroom" "short description of living room"
                       :initial-description "long description of living room"
                       :items #{(it/make ["sofa"] "just a sofa")}
                       :south :bedroom))

(def game-state (make {:bedroom bedroom, :living living} :bedroom #{magazine}))

;; FIXME will have to make bring old mocks as print-line-mock and print-inline mock
(deftest process-input-test
  (testing "unknown command"
    (let [new-state (process-input game-state "dance around")]
      (is-output new-state "I didn't know how to do that.")
      (is (= (assoc new-state :out "") game-state))))

  (testing "look verb"
    (let [new-state (process-input game-state "look ")]
      (is-output new-state
                 ["short description of bedroom"
                  "There was a bed there."
                  "There was a sock there."
                  "There was a drawer there. The drawer contained a pencil"
                  ""
                  "North: ???"])
      (is (= (assoc new-state :out "") (update-in game-state [:moves] inc)))))

  (testing "invalid look with parameters"
    (let [new-state (process-input game-state "look something")]
      (is-output new-state
                 "I didn't know how to do that.")
      (is (= (assoc new-state :out "") game-state))))

  (testing "look at item"
    (let [new-state (process-input game-state "look at bed")]
      (is-output new-state "just a bed")
      (is (= (assoc new-state :out "") (update-in game-state [:moves] inc)))))

  (testing "take item"
    (let [new-state (process-input game-state "take sock")]
      (is-output new-state "Taken.")
      (is (contains? (:inventory new-state) sock))
      (is (not (contains? (:items (current-room new-state)) sock)))))

  (testing "go shortcuts"
    (is-output (process-input game-state "north")
               ["long description of living room"
                "There was a sofa there."])
    (is-output (process-input game-state "n")
               ["long description of living room"
                "There was a sofa there."]))

  (let [chest (it/make ["chest"] "a treasure chest" :closed true :locked true)
        ckey (it/make ["key"] "the chest key" :unlocks chest)
        inventory (conj #{} chest ckey)
        new-state (assoc game-state :inventory inventory)]
    (testing "unlock with item"
      (is-output (process-input new-state "unlock chest with key")
                 "Unlocked."))

    (testing "unlock with no item specified"
      (is-output (process-input new-state "unlock")
                 "Unlock what?")
      (is-output (process-input new-state "unlock chest")
                 "Unlock chest with what?"))))
