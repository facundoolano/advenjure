(ns advenjure.verbs-test
  (:require [clojure.test :refer :all]
            [advenjure.test-utils :refer :all]
            [advenjure.verbs :refer :all]
            [advenjure.utils :refer :all]
            [advenjure.rooms :as room]
            [advenjure.items :as it]))

;;;;;; some test data
(def drawer (it/make ["drawer"] "it's an open drawer." :closed false
                     :items #{(it/make ["key"] "it's a key" :take true)}))
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

(def game-state {:current-room :bedroom
                 :room-map {:bedroom bedroom, :living living}
                 :inventory #{magazine}})

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
      (let [new-state (take_ game-state)]
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
      (let [sack (it/make ["sack"] "a sack" :items #{} :closed true)
            new-state (assoc game-state :inventory #{sack})
            newer-state (open new-state "sack")
            new-sack (it/get-from (:inventory newer-state) "sack")]
        (is-output "The sack is empty.")
        (is (not (:closed new-sack)))))

    (testing "open an already open item"
      (let [sack (it/make ["sack"] "a sack" :items #{} :closed false)
            new-state (assoc game-state :inventory #{sack})
            newer-state (open new-state "sack")]
        (is-output "It's already open.")
        (is (nil? newer-state))))

    (testing "open a non openable item"
      (let [sack (it/make ["sack"] "a sack" :items #{})
            new-state (assoc game-state :inventory #{sack})
            newer-state (open new-state "sack")]
        (is-output "I can't open that.")
        (is (nil? newer-state))))

    (testing "open a missing item"
      (let [new-state (open game-state "sack")]
        (is-output "I don't see that.")
        (is (nil? new-state))))

    (testing "open a container inside a container"
      (let [bottle (it/make ["bottle"] "a bottle" :closed true
                            :items #{(it/make "amount of water")})
            sack (it/make ["sack"] "a sack" :items #{bottle})
            new-state (assoc game-state :inventory #{sack})
            newer-state (open new-state "bottle")
            new-sack (it/get-from (:inventory newer-state) "sack")
            new-bottle (it/get-from (:items new-sack) "bottle")]
        (is-output ["The bottle contains:"
                    "An amount of water"])
        (is (not (:closed new-bottle)))))))

(deftest close-verb
  (with-redefs [say say-mock]
    (testing "close an open item"
      (let [sack (it/make ["sack"] "a sack" :items #{} :closed false)
            new-state (assoc game-state :inventory #{sack})
            newer-state (close new-state "sack")
            new-sack (it/get-from (:inventory newer-state) "sack")]
        (is-output "Closed.")
        (is (:closed new-sack))))

    (testing "close an already closed item"
      (let [sack (it/make ["sack"] "a sack" :items #{} :closed true)
            new-state (assoc game-state :inventory #{sack})
            newer-state (close new-state "sack")]
        (is-output "It's already closed.")
        (is (nil? newer-state))))

    (testing "close a non openable item"
      (let [sack (it/make ["sack"] "a sack" :items #{})
            new-state (assoc game-state :inventory #{sack})
            newer-state (close new-state "sack")]
        (is-output "I can't close that.")
        (is (nil? newer-state))))

    (testing "close a missing item"
      (let [new-state (close game-state "sack")]
        (is-output "I don't see that.")
        (is (nil? new-state))))

    (testing "close a container inside a container"
      (let [bottle (it/make ["bottle"] "a bottle " :closed false
                            :items #{(it/make "amount of water")})
            sack (it/make ["sack"] "a sack" :items #{bottle})
            new-state (assoc game-state :inventory #{sack})
            newer-state (close new-state "bottle")
            new-sack (it/get-from (:inventory newer-state) "sack")
            new-bottle (it/get-from (:items new-sack) "bottle")]
        (is-output "Closed.")
        (is (:closed new-bottle))))))

(deftest unlock-verb
  (with-redefs [say say-mock]
    (let [chest (it/make ["chest"] "a treasure chest" :closed true :locked true)
          ckey (it/make ["key"] "the chest key" :unlocks chest)
          other-key (it/make ["other key"] "another key" :unlocks drawer)
          inventory (conj #{} chest ckey other-key)
          new-state (assoc game-state :inventory inventory)]

      (testing "open a locked item"
        (let [newer-state (open new-state "chest")]
          (is-output "It's locked.")
          (is (nil? newer-state))))

      (testing "unlock a locked item"
        (let [newer-state (unlock new-state "chest" "key")
              new-chest (it/get-from (:inventory newer-state) "chest")]
          (is-output "Unlocked.")
          (is (not (:locked new-chest)))
          (is (nil? (it/get-from (:inventory newer-state) "key"))) ; ASSUMING THE KEY IS DESTROYED AFTER USING
          (open newer-state "chest")
          (is-output "Opened.")))

      (testing "unlock an already unlocked item"
        (let [newer-state (unlock new-state "chest" "key")
              new-chest (it/get-from (:inventory newer-state) "chest")
              last-state (unlock newer-state "chest" "other key")]
          (is-output "It's not locked.")
          (is (nil? last-state))))

      (testing "unlock what?"
        (let [newer-state (unlock new-state)]
          (is-output "Unlock what?")
          (is (nil? newer-state))))

      (testing "unlock with what?"
        (let [newer-state (unlock new-state "chest")]
          (is-output "Unlock chest with what?")
          (is (nil? newer-state))))

      (testing "unlock a non lockable item"
        (let [newer-state (unlock new-state "drawer" "key")]
          (is-output "I can't unlock that.")
          (is (nil? newer-state))))

      (testing "unlock with item that doesn't unlock"
        (let [newer-state (unlock new-state "chest" "sock")]
          (is-output "That doesn't work.")
          (is (nil? newer-state))))

      (testing "unlock with item that unlocks another thing"
        (let [newer-state (unlock new-state "chest" "other key")]
          (is-output "That doesn't work.")
          (is (nil? newer-state)))))))

(deftest read-verb
  (with-redefs [say say-mock]
    (testing "Read a readble item"
      (let [new-state (read_ game-state "magazine")]
        (is-output "Tells the results of every major sports event till the end of the century.")
        (is (nil? new-state))))

    (testing "Read a non readble item"
      (let [new-state (read_ game-state "sock")]
        (is-output "I can't read that.")
        (is (nil? new-state))))))

(deftest inventory-verb
  (with-redefs [say say-mock]
    (testing "list inventory contents"
      (let [new-state (inventory game-state)]
        (is-output ["I'm carrying:" "A magazine"])
        (is (nil? new-state))))

    (testing "empty inventory"
      (let [new-state (assoc game-state :inventory #{})
            newer-state (inventory new-state)]
        (is-output "I'm not carrying anything.")
        (is (nil? newer-state))))

    (testing "list inventory with container"
      (let [bottle {:names ["bottle"] :items #{{:names ["amount of water"]}}}
            sack {:names ["sack"] :items #{bottle}}
            new-state (assoc game-state :inventory #{sack})]
        (inventory new-state)
        (is-output ["I'm carrying:"
                    "A sack. The sack contains:"
                    "A bottle. The bottle contains:"
                    "An amount of water"])))))

(deftest pre-post-conditions
  (with-redefs [say say-mock]
    (testing "Override can't take message"
      (let [new-drawer (assoc drawer :take "It's too heavy to take.")
            new-bedroom (assoc bedroom :items #{new-drawer})
            new-state (assoc-in game-state [:room-map :bedroom] new-bedroom)
            newer-state (take_ new-state "drawer")]
        (is (nil? newer-state))
        (is-output "It's too heavy to take.")))

    (testing "Override look at description"
      (let [new-magazine (assoc magazine :look-at "I don't want to look at it.")
            new-inventory (it/replace-from (:inventory game-state) magazine new-magazine)
            new-state (assoc game-state :inventory new-inventory)
            newer-state (look-at new-state "magazine")]
        (is (nil? newer-state))
        (is-output "I don't want to look at it.")))

    (testing "precondition returns false"
      (let [sock2 (it/make ["other sock"] "another sock"
                           :take `#(contains? (:inventory %) sock))
            new-state (assoc-in game-state [:room-map :bedroom]
                                (room/add-item bedroom sock2))
            newer-state (take_ new-state "other sock")]
        (is (nil? newer-state))
        (is-output "I can't take that.")))

    (testing "precondition returns error message"
      (let [sock2 (it/make ["other sock"] "another sock"
                           :take `#(or (contains? (:inventory %) sock)
                                      "Not unless I have the other sock."))
            new-state (assoc-in game-state [:room-map :bedroom]
                                (room/add-item bedroom sock2))
            newer-state (take_ new-state "other sock")]
        (is (nil? newer-state))
        (is-output "Not unless I have the other sock.")))

    (testing "precondition other syntax"
      (let [sock2 (it/make ["other sock"] "another sock"
                           :take {:pre `#(or (contains? (:inventory %) sock)
                                            "Not unless I have the other sock.")})
            new-state (assoc-in game-state [:room-map :bedroom]
                                (room/add-item bedroom sock2))
            newer-state (take_ new-state "other sock")]
        (is (nil? newer-state))
        (is-output "Not unless I have the other sock.")))

    (testing "precondition returns true"
      (let [sock2 (it/make ["other sock"] "another sock"
                           :take `#(or (contains? (:inventory %) sock)
                                      "Not unless I have the other sock."))
            new-state (assoc-in game-state [:room-map :bedroom]
                                (room/add-item bedroom sock2))
            newer-state (assoc new-state :inventory (conj (:inventory new-state) sock))]
        (take_ newer-state "other sock")
        (is-output "Taken.")))

    (testing "precondition for compound verb"
      (let [beer (it/make ["beer"] "a beer")
            chest (it/make ["chest"] "a treasure chest" :closed true :locked true
                           :unlock `(fn [gs# the-key#]
                                     (or (contains? (:inventory gs#) ~beer)
                                         "Only if I have a beer.")))
            ckey (it/make ["key"] "the chest key" :unlocks chest)
            new-state (assoc game-state :inventory #{chest ckey})]
        (unlock new-state "chest" "key")
        (is-output "Only if I have a beer.")
        (let [newer-state (assoc new-state :inventory #{chest ckey beer})]
          (unlock newer-state "chest" "key")
          (is-output "Unlocked."))))

    (testing "override message for go"
      (let [new-bedroom (assoc bedroom :south "No way I'm going south.")
            new-state (assoc-in game-state [:room-map :bedroom] new-bedroom)
            newer-state (go new-state "south")]
        (is (nil? newer-state))
        (is-output "No way I'm going south.")))

    (testing "precondition for go"
      (let [wallet (it/make "wallet")
            new-bedroom (assoc bedroom :north `#(if (contains? (:inventory %) ~wallet)
                                                 :living
                                                 "Can't leave without my wallet."))
            new-state (assoc-in game-state [:room-map :bedroom] new-bedroom)]
        (go new-state "north")
        (is-output "Can't leave without my wallet.")
        (let [newer-state (assoc new-state :inventory #{wallet})
              last-state (go newer-state "north")]
          (is (= :living (:current-room last-state))))))

    (testing "postcondition replace object"
      (let [broken-bottle (it/make "broken bottle")]
        (defn break-bottle [oldgs newgs]
          (let [inventory (:inventory newgs)
                bottle (it/get-from inventory "bottle")
                new-inv (it/replace-from inventory bottle broken-bottle)]
            (say "I think I broke it.")
            (assoc newgs :inventory new-inv)))

        (let [bottle (it/make ["bottle"] "a bottle"
                              :open {:post `break-bottle})
              new-state (assoc game-state :inventory #{bottle})
              newer-state (open new-state "bottle")]
          (is-output "I think I broke it.")
          (is (contains? (:inventory newer-state) broken-bottle)))))

    (testing "postcondition for compound"
      (def beer (it/make ["beer"] "a beer"))
      (defn get-beer [oldgs newgs]
        (say "There's a beer inside. Taking it.")
        (assoc newgs :inventory (conj (:inventory newgs) beer)))

      (let [chest (it/make ["chest"] "a treasure chest" :closed true :locked true
                           :unlock {:post `get-beer})
            ckey (it/make ["key"] "the chest key" :unlocks chest)
            new-state (assoc game-state :inventory #{chest ckey})
            newer-state (unlock new-state "chest" "key")]
        (is-output ["Unlocked." "There's a beer inside. Taking it."])
        (is (contains? (:inventory newer-state) beer))))

    (testing "postcondition for go"
      (let [new-bedroom (assoc bedroom :north {:pre :living
                                               :post `(fn [oldgs# newgs#] ;empties inv
                                                       (assoc newgs# :inventory #{}))})
            new-state (assoc-in game-state [:room-map :bedroom] new-bedroom)
            newer-state (go new-state "north")]
        (is (empty? (:inventory newer-state)))))))
