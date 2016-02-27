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
              :items #{{:names ["bed"] :description "just a bed"}}})

(def living {:name "Bedroom",
             :full-description "long description of bedroom"
             :short-description "short description of bedroom"
             :items #{{:names ["sofa"] :description "just a sofa"}}})

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



