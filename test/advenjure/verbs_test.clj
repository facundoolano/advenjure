(ns advenjure.verbs-test
  (:require [clojure.test :refer :all]
            [advenjure.verbs :refer :all]))

;;;;; mock println
(def output (atom nil))

(defn say-mock
  ([speech] (say-mock speech nil))
  ([speech game-state] (reset! output speech) game-state))

(defn is-output [expected]
  (is (= @output expected)))

;;;;;; some test data
(def bedroom {:name "Bedroom",
              :full-description "long description of bedroom"
              :short-description "short description of bedroom"
              :items #{{:names ["bed"] :description "just a bed"}}
              :north :living
              :visited true})

(def living {:name "Bedroom",
             :full-description "long description of living room"
             :short-description "short description of living room"
             :items #{{:names ["sofa"] :description "just a sofa"}}
             :south :bedroom})

(def game-state {:current-room :bedroom
                 :room-map {:bedroom bedroom, :living living}
                 :inventory #{{:names ["magazine" "sports magazine"]
                               :description "The cover reads 'Sports Almanac 1950-2000'"}}})

;;;;;;; da tests

(deftest mock-works
  (with-redefs [say say-mock]
    (say "this should be outputed")
    (is-output "this should be outputed")))

(deftest look-verb
  (with-redefs [say say-mock]
    (testing "look at inventory item"
      (look game-state "magazine")
      (is-output "The cover reads 'Sports Almanac 1950-2000'")
      (look game-state "sports magazine")
      (is-output "The cover reads 'Sports Almanac 1950-2000'"))

    (testing "look at room item"
      (look game-state "bed")
      (is-output "just a bed"))

    (testing "look at missing item"
      (look game-state "sofa")
      (is-output "I don't see that."))

    (testing "look at room"
      (look game-state nil)
      (is-output "short description of bedroom")
      (look game-state "")
      (is-output "short description of bedroom"))))


(deftest go-verb
  (with-redefs [say say-mock]
    (let [new-state (go game-state "north")]
      (testing "go to an unvisited room"
        (is-output "long description of living room")
        (is (= (:current-room new-state) :living))
        (is (get-in new-state [:room-map :living :visited])))

      (testing "go to an already visited room"
        (let [newer-state (go new-state "south")]
          (is-output "short description of bedroom")
          (is (= (:current-room newer-state) :bedroom))
          (is (get-in newer-state [:room-map :bedroom :visited])))))

    (testing "go to a blocked direction"
      (go game-state "west")
      (is-output "Can't go in that direction"))

    (testing "go to an invalid direction"
      (go game-state nil)
      (is-output "Go where?")
      (go game-state "crazy")
      (is-output "Go where?"))))


(deftest take-verb
  (with-redefs [say say-mock]
    (testing "take an item from the room") ;puts it in inventory, takes out of room, says "Taken"
    (testing "take an item that's not takable"); says I can't take that, still in room, not in inventory
    (testing "take an item from inventory") ;says "already got it"
    (testing "take an item from other room") ;i dont see that
    (testing "take an invalid item");i dont see that
    (testing "go to an invalid direction")))


