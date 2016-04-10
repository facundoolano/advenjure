(ns advenjure.items-test
  (:require [clojure.test :refer :all]
            [advenjure.items :refer :all]))

(def water (make ["amount of water" "water"]))
(def bottle (make ["bottle"] "a bottle" :items #{water}))
(def sack (make ["sack" "brown sack"] "a sack" :items #{bottle}))
(def empty-sack (assoc sack :items #{}))
(def closed-sack (assoc sack :closed true))
(def sword (make ["sword" "silver sword"]))
(def wsword (make ["sword" "wooden sword"]))

(deftest describe-container-test
  (testing "describe lists items"
    (is (= (describe-container sack)
           "The sack contains a bottle")))

  (testing "describe empty container"
    (is (= (describe-container empty-sack)
           "The sack is empty.")))

  (testing "describe closed container"
    (is (= (describe-container closed-sack)
           "The sack is closed."))))

(deftest get-from-test
  (testing "get top level item"
    (let [item-set #{sack sword}]
      (is (= (get-from item-set "sack") [sack]))
      (is (= (get-from item-set "brown sack") [sack]))
      (is (= (get-from item-set "silver sword") [sword]))))

  (testing "get inner items"
    (let [item-set #{sack sword}]
      (is (= (get-from item-set "bottle") [bottle]))
      (is (= (get-from item-set "water") [water]))))

  (testing "don't get from closed container"
    (let [item-set #{closed-sack sword}]
      (is (empty? (get-from item-set "bottle")))
      (is (empty? (get-from item-set "water")))))

  (testing "don't get from partial match"
    (let [item-set #{closed-sack sword}]
      (is (empty? (get-from item-set "silver")))))

  (testing "get multiple items"
    (let [item-set #{wsword sword}]
      (is (= (get-from item-set "silver sword") [sword]))
      (is (= (get-from item-set "wooden sword") [wsword]))
      (is (= (get-from item-set "sword") [sword wsword])))))

