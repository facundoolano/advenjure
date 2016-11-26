(ns advenjure.game-test
  (:require [clojure.test :refer :all]
            [advenjure.test-utils :refer :all]
            [advenjure.utils :refer [say current-room]]
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


(deftest process-input-test
  (with-redefs [advenjure.ui.output/print-line say-mock]

    (testing "unknown command"
      (let [new-state (process-input game-state "dance around")]
        (is-output "I didn't know how to do that.")
        (is (= new-state game-state))))

    (testing "look verb"
      (let [new-state (process-input game-state "look ")]
        (is-output ["short description of bedroom"
                    "There was a bed there."
                    "There was a sock there."
                    "There was a drawer there. The drawer contained a pencil"])
        (is (= new-state (update-in game-state [:moves] inc)))))

    (testing "invalid look with parameters"
      (let [new-state (process-input game-state "look something")]
        (is-output "I didn't know how to do that.")
        (is (= new-state game-state))))

    (testing "look at item"
      (let [new-state (process-input game-state "look at bed")]
        (is-output "just a bed")
        (is (= new-state (update-in game-state [:moves] inc)))))

    (testing "take item"
      (let [new-state (process-input game-state "take sock")]
        (is-output "Taken.")
        (is (contains? (:inventory new-state) sock))
        (is (not (contains? (:items (current-room new-state)) sock)))))

    (testing "go shortcuts"
      (process-input game-state "north")
      (is-output ["long description of living room"
                  "There was a sofa there."])
      (process-input game-state "n")
      (is-output ["long description of living room"
                  "There was a sofa there."]))

    (let [chest (it/make ["chest"] "a treasure chest" :closed true :locked true)
          ckey (it/make ["key"] "the chest key" :unlocks chest)
          inventory (conj #{} chest ckey)
          new-state (assoc game-state :inventory inventory)]
      (testing "unlock with item"
        (process-input new-state "unlock chest with key")
        (is-output "Unlocked."))

      (testing "unlock with no item specified"
        (process-input new-state "unlock")
        (is-output "Unlock what?")
        (process-input new-state "unlock chest")
        (is-output "Unlock chest with what?")))))
