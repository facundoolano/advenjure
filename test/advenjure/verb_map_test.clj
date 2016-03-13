(ns advenjure.verb-map-test
  (:require [clojure.test :refer :all]
            [advenjure.verb-map :refer :all]))


(def test-map (-> {}
                  (add-verb ["^take (.*)" "^get (.*)"] #(str "take"))
                  (add-verb ["^north$"] #(str "go north"))
                  (add-verb ["^unlock (.*) with (.*)"] #(str "unlock"))))
(def sorted-test (reverse (sort-by count (keys test-map))))


(deftest verb-match-test
  (with-redefs [verb-map test-map
                sorted-verbs sorted-test]

    (testing "simple verb match"
      (let [[verb tokens] (find-verb "take magazine")]
        (is (= verb "^take (.*)"))
        (is (= tokens (list "magazine")))))

    (testing "simple synonym match"
      (let [[verb tokens] (find-verb "get magazine")]
        (is (= verb "^get (.*)"))
        (is (= tokens (list "magazine")))))

    (testing "initial garbage mismatch"
      (let [[verb tokens] (find-verb "lalala get magazine")]
        (is (nil? verb))
        (is (nil? tokens))))

    (testing "no parameter match"
      (let [[verb tokens] (find-verb "north")]
        (is (= verb "^north$"))
        (is (= tokens (list)))))

    (testing "no parameter mismatch"
      (let [[verb tokens] (find-verb "north go now")]
        (is (nil? verb))
        (is (nil? tokens))))

    (testing "compound verb match"
      (let [[verb tokens] (find-verb "unlock door with key")]
        (is (= verb "^unlock (.*) with (.*)"))
        (is (= tokens (list "door" "key")))))))
