(ns advenjure.verbs-test
  (:require [clojure.test :refer :all]
            [advenjure.verbs :refer :all]
            [advenjure.rooms :as room]
            [advenjure.items :as it]))

;;;;; mock println
(def output (atom nil))

(defn say-mock
  "Save the speech lines to output, separated by '\n'"
  ([speech] (reset! output (clojure.string/join "\n" [@output speech])) nil))

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
(def drawer (it/make ["drawer"] "it's an open drawer." :closed false
                     :items #{(it/make ["key"] "it's a key" :take true)}))
(def sock (it/make ["sock"] "a sock" :take true))
(def bedroom (room/make "Bedroom" "short description of bedroom"
              :initial-description "long description of bedroom"
              :items #{(it/make ["bed"] "just a bed") drawer sock}
              :north :living
              :visited true))

(def living (room/make "Bedroom" "short description of living room"
             :initial-description "long description of living room"
             :items #{(it/make ["sofa"] "just a sofa")}
             :south :bedroom))

(def game-state {:current-room :bedroom
                 :room-map {:bedroom bedroom, :living living}
                 :inventory #{(it/make ["magazine" "sports magazine"]
                               "The cover reads 'Sports Almanac 1950-2000'"
                               :take true)}})

;;;;;;; da tests

(deftest mock-works
  (with-redefs [say say-mock]
    (say "this should be outputed")
    (is-output "this should be outputed")))

(deftest look-verb
  (with-redefs [say say-mock]

    (testing "look at room"
      (look game-state)
      (is-output ["short description of bedroom"
                  "There's a bed here."
                  "There's a sock here."
                  "There's a drawer here. The drawer contains:"
                  "A key"]))))

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
      (let [coin {:names ["coin"] :description "a nickle"}
            sack {:names ["sack"] :items #{coin} :closed true}
            new-state (assoc game-state :inventory #{sack})]
       (look-at new-state "coin")
       (is-output "I don't see that.")))

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

    (testing "look in container inside container"
      (let [bottle {:names ["bottle"] :items #{{:names ["amount of water"]}}}
            sack {:names ["sack"] :items #{bottle}}
            new-state (assoc game-state :inventory #{sack})]
        (look-inside new-state "bottle")
        (is-output ["The bottle contains:" "An amount of water"])))

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
                      "There's a sock here."
                      "There's a drawer here. The drawer contains:"
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
    (testing "take an item from the room"
      (let [new-state (take_ game-state "sock")]
        (is (it/get-from (:inventory new-state) "sock"))
        (is (not (it/get-from (:items (current-room new-state)) "sock")))
        (is-output "Taken.")))

    (testing "take an item that's not takable"
      (let [new-state (take_ game-state "bed")]
        (is (nil? new-state))
        (is-output "I can't take that.")))

    (testing "take an item from inventory"
      (let [new-state (take_ game-state "magazine")]
        (is (nil? new-state))
        (is-output "I already got that.")))

    (testing "take an invalid item"
      (let [new-state (take_ game-state "microwave")]
        (is (nil? new-state))
        (is-output "I don't see that.")))

    (testing "take with no parameter"
      (let [new-state (take_ game-state "")]
        (is (nil? new-state))
        (is-output "Take what?")))

    (testing "take an item from other room"
      (let [new-state (assoc game-state :current-room :living)
            newer-state (take_ new-state "sock")]
        (is (nil? newer-state))
        (is-output "I don't see that.")))

    (testing "take item in room container"
      (let [new-state (take_ game-state "key")]
        (is (it/get-from (:inventory new-state) "key"))
        (is (not (it/get-from (:items (current-room new-state)) "key")))
        (is-output "Taken.")))

    (testing "take item in inv container"
      (let [coin {:names ["coin"] :description "a nickle" :take true}
            sack {:names ["sack"] :items #{coin}}
            new-state (assoc game-state :inventory #{sack})
            newer-state (take_ new-state "coin")
            inv (:inventory newer-state)
            new-sack (it/get-from inv "sack")]
        (is (contains? inv coin))
        (is (not (contains? (:items new-sack) coin)))
        (is-output "Taken.")))

    (testing "take item in closed container"
      (let [coin {:names ["coin"] :description "a nickle" :take true}
            sack {:names ["sack"] :items #{coin} :closed true}
            new-state (assoc game-state :inventory #{sack})
            newer-state (take_ new-state "coin")]
        (is (nil? newer-state))
        (is-output "I don't see that.")))))

(deftest open-verb
  (with-redefs [say say-mock]
    (testing "open a closed item"
      (let [sack {:names ["sack"] :items #{} :closed true}
            new-state (assoc game-state :inventory #{sack})
            newer-state (open new-state "sack")
            new-sack (it/get-from (:inventory newer-state) "sack")]
        (is-output "Opened.")
        (is (not (:closed new-sack)))))

    (testing "open an already open item"
      (let [sack {:names ["sack"] :items #{} :closed false}
            new-state (assoc game-state :inventory #{sack})
            newer-state (open new-state "sack")]
        (is-output "It's already open.")
        (is (nil? newer-state))))

    (testing "open a non openable item"
      (let [sack {:names ["sack"] :items #{}}
            new-state (assoc game-state :inventory #{sack})
            newer-state (open new-state "sack")]
        (is-output "I can't open that.")
        (is (nil? newer-state))))

    (testing "open a missing item"
      (let [new-state (open game-state "sack")]
        (is-output "I don't see that.")
        (is (nil? new-state))))

    (testing "open a container inside a container"
      (let [bottle {:names ["bottle"] :closed true :items #{{:names ["amount of water"]}}}
            sack {:names ["sack"] :items #{bottle}}
            new-state (assoc game-state :inventory #{sack})
            newer-state (open new-state "bottle")
            new-sack (it/get-from (:inventory newer-state) "sack")
            new-bottle (it/get-from (:items new-sack) "bottle")]
        (is-output "Opened.")
        (is (not (:closed new-bottle)))))))


(deftest close-verb
  (with-redefs [say say-mock]
    (testing "close an open item"
      (let [sack {:names ["sack"] :items #{} :closed false}
            new-state (assoc game-state :inventory #{sack})
            newer-state (close new-state "sack")
            new-sack (it/get-from (:inventory newer-state) "sack")]
        (is-output "Closed.")
        (is (:closed new-sack))))

    (testing "close an already closed item"
      (let [sack {:names ["sack"] :items #{} :closed true}
            new-state (assoc game-state :inventory #{sack})
            newer-state (close new-state "sack")]
        (is-output "It's already closed.")
        (is (nil? newer-state))))

    (testing "close a non openable item"
      (let [sack {:names ["sack"] :items #{}}
            new-state (assoc game-state :inventory #{sack})
            newer-state (close new-state "sack")]
        (is-output "I can't close that.")
        (is (nil? newer-state))))

    (testing "close a missing item"
      (let [new-state (close game-state "sack")]
        (is-output "I don't see that.")
        (is (nil? new-state))))

    (testing "close a container inside a container"
      (let [bottle {:names ["bottle"] :closed false :items #{{:names ["amount of water"]}}}
            sack {:names ["sack"] :items #{bottle}}
            new-state (assoc game-state :inventory #{sack})
            newer-state (close new-state "bottle")
            new-sack (it/get-from (:inventory newer-state) "sack")
            new-bottle (it/get-from (:items new-sack) "bottle")]
        (is-output "Closed.")
        (is (:closed new-bottle))))))


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

(deftest process-input-test
  (with-redefs [say say-mock]

    (testing "unknown command"
      (let [new-state (process-input game-state "dance around")]
        (is-output "I don't know how to do that.")
        (is (= new-state game-state))))

    (testing "look verb"
      (let [new-state (process-input game-state "look ")]
        (is-output ["short description of bedroom"
                    "There's a bed here."
                    "There's a sock here."
                    "There's a drawer here. The drawer contains:"
                    "A key"])
        (is (= new-state game-state))))

    (testing "invalid look with parameters"
      (let [new-state (process-input game-state "look something")]
        (is-output "I don't know how to do that.")
        (is (= new-state game-state))))

    (testing "look at item"
      (let [new-state (process-input game-state "look at bed")]
        (is-output "just a bed")
        (is (= new-state game-state))))

    (testing "take item"
      (let [new-state (process-input game-state "take sock")]
        (is-output "Taken.")
        (is (contains? (:inventory new-state) sock))
        (is (not (contains? (:items (current-room new-state)) sock)))))))

