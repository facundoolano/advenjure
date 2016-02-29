(ns advenjure.verbs-test
  (:require [clojure.test :refer :all]
            [advenjure.verbs :refer :all]))

;;;;; mock println
(def output (atom nil))

(defn say-mock
  "Save the speech lines to output, separated by '\n'"
  ([speech] (say-mock speech nil))
  ([speech game-state] (reset! output (clojure.string/join "\n" [@output speech])) game-state))

(defn is-output
  "Compare the last n output lines with the given."
  ([expected]
   (let [as-seq (if (string? expected)
                  (list expected)
                  (seq expected))
         lines (count as-seq)
         results (take-last lines (clojure.string/split-lines @output))]
     (is (= results as-seq)))))

(use-fixtures :each (fn [f]
                      (reset! output nil)
                      (f)))

;;;;;; some test data
(def drawer {:names ["drawer"] :description "it's an open drawer."
             :open true :items #{{:names ["key"] :description "it's a key" :take true}}})

(def bedroom {:name "Bedroom",
              :initial-description "long description of bedroom"
              :description "short description of bedroom"
              :items #{{:names ["bed"] :description "just a bed"}
                       drawer}
              :north :living
              :visited true})

(def living {:name "Bedroom",
             :initial-description "long description of living room"
             :description "short description of living room"
             :items #{{:names ["sofa"] :description "just a sofa"}}
             :south :bedroom})

(def game-state {:current-room :bedroom
                 :room-map {:bedroom bedroom, :living living}
                 :inventory #{{:names ["magazine" "sports magazine"]
                               :description "The cover reads 'Sports Almanac 1950-2000'"
                               :take true}}})

;;;;;;; da tests

(deftest mock-works
  (with-redefs [say say-mock]
    (say "this should be outputed")
    (is-output "this should be outputed")))

(deftest look-verb
  (with-redefs [say say-mock]

    (testing "look at room"
      (look game-state nil)
      (is-output ["short description of bedroom"
                  "There's a bed here."
                  "There's a drawer here."
                  "The drawer contains:"
                  "A key"])
      (look game-state "")
      (is-output ["short description of bedroom"
                  "There's a bed here."
                  "There's a drawer here."
                  "The drawer contains:"
                  "A key"]))

    (testing "invalid look command"
      (look game-state "bed")
      (is-output "I understood as far as 'look'"))))


(deftest look-at-verb
  (with-redefs [say say-mock]
    (testing "look at inventory item"
      (look-at game-state "magazine")
      (is-output "The cover reads 'Sports Almanac 1950-2000'")
      (look-at game-state "sports magazine")
      (is-output "The cover reads 'Sports Almanac 1950-2000'"))

    (testing "look at room item"
      (look-at game-state "bed")
      (is-output "just a bed"))

    (testing "look at inventory container item"
      (let [coin {:names ["coin"] :description "a nickle"}
            sack {:names ["sack"] :items #{coin}}
            new-state (assoc game-state :inventory #{sack})]
       (look-at new-state "coin")
       (is-output "a nickle")))

    (testing "look at room container item"
      (look-at game-state "key")
      (is-output "it's a key"))

    (testing "look at closed container item"
      (is false))

    (testing "look at missing item"
      (look-at game-state "sofa")
      (is-output "I don't see that."))

    (testing "look at container still describes"
      (look-at game-state "drawer")
      (is-output "it's an open drawer."))))


(deftest look-inside-verb
  (with-redefs [say say-mock]
    (testing "look in container lists contents"
      (look-inside game-state "drawer")
      (is-output ["The drawer contains:" "A key"]))

    (testing "look in empty container"
      (let [sack {:names ["sack"] :items #{}}
            new-state (assoc game-state :inventory #{sack})]
        (look-inside new-state "sack")
        (is-output "The sack is empty.")))

    (testing "look in container inside container"
      (let [bottle {:names ["bottle"] :items #{{:names ["amount of water"]}}}
            sack {:names ["sack"] :items #{bottle}}
            new-state (assoc game-state :inventory #{sack})]
        (look-inside new-state "bottle")
        (is-output ["The bottle contains:" "An amount of water"])))

    (testing "look in closed container"
      (is false))

    (testing "look inside non-container"
      (look-inside game-state "bed")
      (is-output "I can't look inside a bed."))))


(deftest go-verb
  (with-redefs [say say-mock]
    (let [new-state (go game-state "north")]
      (testing "go to an unvisited room"
        (is-output ["long description of living room"
                    "There's a sofa here."])
        (is (= (:current-room new-state) :living))
        (is (get-in new-state [:room-map :living :visited])))

      (testing "go to an already visited room"
        (let [newer-state (go new-state "south")]
          (is-output ["short description of bedroom"
                      "There's a bed here."
                      "There's a drawer here."
                      "The drawer contains:"
                      "A key"])
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
    (testing "take item in room container")
    (testing "take item in inv container")
    (testing "take item in open container")
    (testing "take item in closed container")))



