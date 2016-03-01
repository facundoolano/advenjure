(ns advenjure.items-test
  (:require [clojure.test :refer :all]
            [advenjure.items :refer :all]))


(def sack {:names ["sack"] :items #{{:names ["bottle"] :items #{{:names ["amount of water"]}}}}})
(def empty-sack {:names ["sack"] :items #{}})
(def closed-sack {:names ["sack"] :closed true :items #{{:names ["bottle"] :items #{{:names ["amount of water"]}}}}})

(deftest describe-container-test
  (testing "describe lists items"
    (is (= (describe-container sack)
          "The sack contains:\nA bottle. The bottle contains:\nAn amount of water")))
  (testing "describe empty container"
    (is (= (describe-container empty-sack)
          "The sack is empty.")))
  (testing "describe closed container"
    (is (= (describe-container closed-sack)
           "The sack is closed."))))