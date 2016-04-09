(ns advenjure.items-test
  (:require [clojure.test :refer :all]
            [advenjure.items :refer :all]))

(def sack (make ["sack"] "a sack"
                :items #{(make ["bottle"] "a bottle"
                               :items #{(make "amount of water")})}))
(def empty-sack (make ["sack"] "a sack" :items #{}))
(def closed-sack (make ["sack"] "a sack"
                       :closed true :items #{(make ["bottle"] "a bottle"
                                                   :items #{(make "amount of water")})}))

(deftest describe-container-test
  (testing "describe lists items"
    (is (= (describe-container sack)
           "The sack contains:\n  A bottle. The bottle contains:\n    An amount of water")))

  (testing "describe empty container"
    (is (= (describe-container empty-sack)
           "The sack is empty.")))

  (testing "describe closed container"
    (is (= (describe-container closed-sack)
           "The sack is closed."))))
