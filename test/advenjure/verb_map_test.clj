(ns advenjure.verb-map-test
  (:require [clojure.test :refer :all]
            [advenjure.verb-map :refer [find-verb expand-verb]]))

(def test-map (merge (expand-verb {:commands ["take (.*)" "get (.*)"] :handler #(str "take")})
                     (expand-verb {:commands ["north"] :handler #(str "go north")})
                     (expand-verb {:commands ["unlock (.*) with (.*)"] :handler #(str "unlock")})))

(deftest verb-match-test
  (testing "simple verb match"
    (let [[verb tokens] (find-verb test-map "take magazine")]
      (is (= verb "^take (.*)$"))
      (is (= tokens (list "magazine")))))

  (testing "simple synonym match"
    (let [[verb tokens] (find-verb test-map "get magazine")]
      (is (= verb "^get (.*)$"))
      (is (= tokens (list "magazine")))))

  (testing "initial garbage mismatch"
    (let [[verb tokens] (find-verb test-map "lalala get magazine")]
      (is (nil? verb))
      (is (nil? tokens))))

  (testing "no parameter match"
    (let [[verb tokens] (find-verb test-map "north")]
      (is (= verb "^north$"))
      (is (= tokens (list)))))

  (testing "no parameter mismatch"
    (let [[verb tokens] (find-verb test-map "north go now")]
      (is (nil? verb))
      (is (nil? tokens))))

  (testing "compound verb match"
    (let [[verb tokens] (find-verb test-map "unlock door with key")]
      (is (= verb "^unlock (.*) with (.*)$"))
      (is (= tokens (list "door" "key"))))))
