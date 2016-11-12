(ns advenjure.verbs-test
  (:require [clojure.test :refer :all]
            [advenjure.test-utils :refer :all]
            [advenjure.verbs :refer :all]
            [advenjure.utils :refer :all]
            [advenjure.rooms :as room]
            [advenjure.items :as it]))

;;;;;; some test data
(def drawer (it/make ["drawer"] "it's an open drawer." :closed false
                     :items #{(it/make "pencil" "it's a pencil" :take true)}))
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
                  "There was a bed there."
                  "There was a sock there."
                  "There was a drawer there. The drawer contained a pencil"]))))

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
      (look-at game-state "pencil")
      (is-output "it's a pencil"))

    (testing "look at closed container item"
      (let [coin {:names ["coin"] :description "a nickle"}
            sack {:names ["sack"] :items #{coin} :closed true}
            new-state (assoc game-state :inventory #{sack})]
        (look-at new-state "coin")
        (is-output "I didn't see that.")))

    (testing "look at ambiguous item name"
      (let [red-shoe (it/make ["shoe" "red shoe"] "it's red")
            brown-shoe (it/make ["shoe" "brown shoe"] "an old brown shoe")
            new-state (assoc game-state :inventory #{red-shoe brown-shoe})]
        (look-at new-state "shoe")
        (is-output "Which shoe? The brown shoe or the red shoe?")
        (look-at new-state "brown shoe")
        (is-output "an old brown shoe"))
      (let [red-shoe (it/make ["shoe" "red shoe"] "it's red")
            brown-shoe (it/make ["shoe" "brown shoe"] "an old brown shoe")
            green-shoe (it/make ["shoe" "green shoe"] "an old green shoe")
            new-state (assoc game-state :inventory #{red-shoe brown-shoe green-shoe})]
        (look-at new-state "shoe")
        (is-output "Which shoe? The brown shoe, the green shoe or the red shoe?")
        (look-at new-state "brown shoe")
        (is-output "an old brown shoe")))

    (testing "look at missing item"
      (look-at game-state "sofa")
      (is-output "I didn't see that."))

    (testing "look at container still describes"
      (look-at game-state "drawer")
      (is-output "it's an open drawer."))))

(deftest look-inside-verb
  (with-redefs [say say-mock]
    (testing "look in container lists contents"
      (look-inside game-state "drawer")
      (is-output ["The drawer contained a pencil"]))

    (testing "look in container inside container"
      (let [bottle {:names ["bottle"] :items #{{:names ["amount of water"]}}}
            sack {:names ["sack"] :items #{bottle}}
            new-state (assoc game-state :inventory #{sack})]
        (look-inside new-state "bottle")
        (is-output ["The bottle contained an amount of water"])))

    (testing "look inside non-container"
      (look-inside game-state "bed")
      (is-output "I couldn't look inside a bed."))))

(deftest go-verb
  (with-redefs [say say-mock
                advenjure.map/print-map_ identity]
    (let [new-state (go game-state "north")]
      (testing "go to an unvisited room"
        (is-output ["long description of living room"
                    "There was a sofa there."])
        (is (= (:current-room new-state) :living))
        (is (get-in new-state [:room-map :living :visited])))

      (testing "go to an already visited room"
        (let [newer-state (go new-state "south")]
          (is-output ["short description of bedroom"
                      "There was a bed there."
                      "There was a sock there."
                      "There was a drawer there. The drawer contained a pencil"])
          (is (= (:current-room newer-state) :bedroom))
          (is (get-in newer-state [:room-map :bedroom :visited]))))

      (testing "go to a visited room name"
        (let [newer-state (go new-state "bedroom")]
            (is-output ["short description of bedroom"
                        "There was a bed there."
                        "There was a sock there."
                        "There was a drawer there. The drawer contained a pencil"])
            (is (= (:current-room newer-state) :bedroom))
            (is (get-in newer-state [:room-map :bedroom :visited])))))

    (testing "go to a blocked direction"
      (go game-state "west")
      (is-output "couldn't go in that direction"))

    (testing "go to an invalid direction"
      (go game-state nil)
      (is-output "Go where?")
      (go game-state "crazy")
      (is-output "Go where?"))))

(deftest go-back-verb
  (with-redefs [say say-mock
                advenjure.map/print-map_ identity]
    (testing "Should remember previous room and go back"
      (let [new-state (-> game-state
                          (go "north")
                          (go-back))]
        (is (:current-room new-state) :bedroom)
        (is (:previous-room new-state) :living)
        (is-output ["long description of living room"
                    "There was a sofa there."
                    "short description of bedroom"
                    "There was a bed there."
                    "There was a sock there."
                    "There was a drawer there. The drawer contained a pencil"])))

    (testing "Should say can't go back if not known previous location")

    (testing "Should say can't go back if previous room not currently accesible")))


(deftest take-verb
  (with-redefs [say say-mock]
    (testing "take an item from the room"
      (let [new-state (take_ game-state "sock")]
        (is (it/get-from (:inventory new-state) "sock"))
        (is (empty? (it/get-from (:items (current-room new-state)) "sock")))
        (is-output "Taken.")))

    (testing "take an item that's not takable"
      (let [new-state (take_ game-state "bed")]
        (is (nil? new-state))
        (is-output "I couldn't take that.")))

    (testing "take an item from inventory"
      (let [new-state (take_ game-state "magazine")]
        (is (nil? new-state))
        (is-output "I already had that.")))

    (testing "take an invalid item"
      (let [new-state (take_ game-state "microwave")]
        (is (nil? new-state))
        (is-output "I didn't see that.")))

    (testing "take with no parameter"
      (let [new-state (take_ game-state)]
        (is (nil? new-state))
        (is-output "Take what?")))

    (testing "take an item from other room"
      (let [new-state (assoc game-state :current-room :living)
            newer-state (take_ new-state "sock")]
        (is (nil? newer-state))
        (is-output "I didn't see that.")))

    (testing "take item in room container"
      (let [new-state (take_ game-state "pencil")]
        (is (it/get-from (:inventory new-state) "pencil"))
        (is (empty? (it/get-from (:items (current-room new-state)) "pencil")))
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
        (is-output "I didn't see that.")))))

(deftest take-verb
  (with-redefs [say say-mock say-inline say-inline-mock]
    (testing "take all items in the room with containers, ignore inventory"
      (let [new-state (take-all game-state)
            item-names (set (map #(first (:names %)) (:inventory new-state)))]
        (is (= item-names #{"magazine" "pencil" "sock"}))
        ;; lousy, assumes some order in items
        (is-output ["Sock: Taken."
                    "Pencil: Taken."])))

    (testing "attempt taking items that define :take property"
      (let [new-bedroom (-> bedroom
                            (room/add-item (it/make "shoe" "a shoe" :take "I didn't want that."))
                            (room/add-item (it/make "fridge" "a fridge" :take false)))
            new-state (-> game-state
                          (assoc-in [:room-map :bedroom] new-bedroom)
                          (take-all))
            item-names (set (map #(first (:names %)) (:inventory new-state)))]
        (is (= item-names #{"magazine" "pencil" "sock"}))
        ;; lousy, assumes some order in items
        (is-output ["Sock: Taken."
                    "Shoe: I didn't want that."
                    "Fridge: I couldn't take that."
                    "Pencil: Taken."])))

    (testing "take all when no items left"
      (let [empty-state (assoc-in game-state [:room-map :bedroom :items] #{})
            new-state (take-all empty-state)]
        (is-output "I saw nothing worth taking.")
        (is (nil? new-state))))))


(deftest open-verb
  (with-redefs [say say-mock]
    (testing "open a closed item"
      (let [sack (it/make ["sack"] "a sack" :items #{} :closed true)
            new-state (assoc game-state :inventory #{sack})
            newer-state (open new-state "sack")
            new-sack (it/get-from (:inventory newer-state) "sack")]
        (is-output "The sack was empty.")
        (is (not (:closed new-sack)))))

    (testing "open an already open item"
      (let [sack (it/make ["sack"] "a sack" :items #{} :closed false)
            new-state (assoc game-state :inventory #{sack})
            newer-state (open new-state "sack")]
        (is-output "It was already open.")
        (is (nil? newer-state))))

    (testing "open a non openable item"
      (let [sack (it/make ["sack"] "a sack" :items #{})
            new-state (assoc game-state :inventory #{sack})
            newer-state (open new-state "sack")]
        (is-output "I couldn't open that.")
        (is (nil? newer-state))))

    (testing "open a missing item"
      (let [new-state (open game-state "sack")]
        (is-output "I didn't see that.")
        (is (nil? new-state))))

    (testing "open a container inside a container"
      (let [bottle (it/make ["bottle"] "a bottle" :closed true
                            :items #{(it/make "amount of water")})
            sack (it/make ["sack"] "a sack" :items #{bottle})
            new-state (assoc game-state :inventory #{sack})
            newer-state (open new-state "bottle")
            new-sack (it/get-from (:inventory newer-state) "sack")
            new-bottle (it/get-from (:items new-sack) "bottle")]
        (is-output ["The bottle contained an amount of water"])
        (is (not (:closed new-bottle)))))))

(deftest close-verb
  (with-redefs [say say-mock]
    (testing "close an open item"
      (let [sack (it/make ["sack"] "a sack" :items #{} :closed false)
            new-state (assoc game-state :inventory #{sack})
            newer-state (close new-state "sack")
            new-sack (first (it/get-from (:inventory newer-state) "sack"))]
        (is-output "Closed.")
        (is (:closed new-sack))))

    (testing "close an already closed item"
      (let [sack (it/make ["sack"] "a sack" :items #{} :closed true)
            new-state (assoc game-state :inventory #{sack})
            newer-state (close new-state "sack")]
        (is-output "It was already closed.")
        (is (nil? newer-state))))

    (testing "close a non openable item"
      (let [sack (it/make ["sack"] "a sack" :items #{})
            new-state (assoc game-state :inventory #{sack})
            newer-state (close new-state "sack")]
        (is-output "I couldn't close that.")
        (is (nil? newer-state))))

    (testing "close a missing item"
      (let [new-state (close game-state "sack")]
        (is-output "I didn't see that.")
        (is (nil? new-state))))

    (testing "close a container inside a container"
      (let [bottle (it/make ["bottle"] "a bottle " :closed false
                            :items #{(it/make "amount of water")})
            sack (it/make ["sack"] "a sack" :items #{bottle})
            new-state (assoc game-state :inventory #{sack})
            newer-state (close new-state "bottle")
            new-sack (first (it/get-from (:inventory newer-state) "sack"))
            new-bottle (first (it/get-from (:items new-sack) "bottle"))]
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
          (is-output "It was locked.")
          (is (nil? newer-state))))

      (testing "unlock a locked item"
        (let [newer-state (unlock new-state "chest" "key")
              new-chest (it/get-from (:inventory newer-state) "chest")]
          (is-output "Unlocked.")
          (is (not (:locked new-chest)))
          (open newer-state "chest")
          (is-output "Opened.")))

      (testing "unlock an already unlocked item"
        (let [newer-state (unlock new-state "chest" "key")
              new-chest (it/get-from (:inventory newer-state) "chest")
              last-state (unlock newer-state "chest" "other key")]
          (is-output "It wasn't locked.")
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
          (is-output "I couldn't unlock that.")
          (is (nil? newer-state))))

      (testing "unlock with item that didn't unlock"
        (let [newer-state (unlock new-state "chest" "sock")]
          (is-output "That didn't work.")
          (is (nil? newer-state))))

      (testing "unlock with item that unlocks another thing"
        (let [newer-state (unlock new-state "chest" "other key")]
          (is-output "That didn't work.")
          (is (nil? newer-state)))))))

(deftest read-verb
  (with-redefs [say say-mock]
    (testing "Read a readble item"
      (let [new-state (read_ game-state "magazine")]
        (is-output "Tells the results of every major sports event till the end of the century.")
        (is (nil? new-state))))

    (testing "Read a non readble item"
      (let [new-state (read_ game-state "sock")]
        (is-output "I couldn't read that.")
        (is (nil? new-state))))))

(deftest inventory-verb
  (with-redefs [say say-mock]
    (testing "list inventory contents"
      (let [new-state (inventory game-state)]
        (is-output ["I was carrying:" "A magazine"])
        (is (nil? new-state))))

    (testing "empty inventory"
      (let [new-state (assoc game-state :inventory #{})
            newer-state (inventory new-state)]
        (is-output "I wasn't carrying anything.")
        (is (nil? newer-state))))

    (testing "list inventory with container"
      (let [bottle {:names ["bottle"] :items #{{:names ["amount of water"]}}}
            sack {:names ["sack"] :items #{bottle}}
            new-state (assoc game-state :inventory #{sack})]
        (inventory new-state)
        (is-output ["I was carrying:"
                    "A sack. The sack contained a bottle"])))))

(deftest pre-post-conditions
  (with-redefs [say say-mock
                advenjure.map/print-map_ identity]
    (testing "Override couldn't take message"
      (let [new-drawer (assoc drawer :take "It's too heavy to take.")
            new-bedroom (assoc bedroom :items #{new-drawer})
            new-state (assoc-in game-state [:room-map :bedroom] new-bedroom)
            newer-state (take_ new-state "drawer")]
        (is (nil? newer-state))
        (is-output "It's too heavy to take.")))

    (testing "Override look at description"
      (let [new-magazine (assoc magazine :look-at "I didn't want to look at it.")
            new-inventory (it/replace-from (:inventory game-state) magazine new-magazine)
            new-state (assoc game-state :inventory new-inventory)
            newer-state (look-at new-state "magazine")]
        (is (nil? newer-state))
        (is-output "I didn't want to look at it.")))

    (testing "precondition returns false"
      (let [sock2 (it/make ["other sock"] "another sock"
                           :take `#(contains? (:inventory %) sock))
            new-state (assoc-in game-state [:room-map :bedroom]
                                (room/add-item bedroom sock2))
            newer-state (take_ new-state "other sock")]
        (is (nil? newer-state))
        (is-output "I couldn't take that.")))

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
      (let [new-bedroom (assoc bedroom :south "No way I was going south.")
            new-state (assoc-in game-state [:room-map :bedroom] new-bedroom)
            newer-state (go new-state "south")]
        (is (nil? newer-state))
        (is-output "No way I was going south.")))

    (testing "precondition for go"
      (let [wallet (it/make "wallet")
            new-bedroom (assoc bedroom :north `#(if (contains? (:inventory %) ~wallet)
                                                 :living
                                                 "couldn't leave without my wallet."))
            new-state (assoc-in game-state [:room-map :bedroom] new-bedroom)]
        (go new-state "north")
        (is-output "couldn't leave without my wallet.")
        (let [newer-state (assoc new-state :inventory #{wallet})
              last-state (go newer-state "north")]
          (is (= :living (:current-room last-state))))))

    (testing "postcondition replace object"
      (let [broken-bottle (it/make "broken bottle")]
        (defn break-bottle [oldgs newgs]
          (let [inventory (:inventory newgs)
                bottle (first (it/get-from inventory "bottle"))
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
        (say "There was a beer inside. Taking it.")
        (assoc newgs :inventory (conj (:inventory newgs) beer)))

      (let [chest (it/make ["chest"] "a treasure chest" :closed true :locked true
                           :unlock {:post `get-beer})
            ckey (it/make ["key"] "the chest key" :unlocks chest)
            new-state (assoc game-state :inventory #{chest ckey})
            newer-state (unlock new-state "chest" "key")]
        (is-output ["Unlocked." "There was a beer inside. Taking it."])
        (is (contains? (:inventory newer-state) beer))))

    (testing "postcondition for go"
      (let [new-bedroom (assoc bedroom :north {:pre :living
                                               :post `(fn [oldgs# newgs#] ;empties inv
                                                       (assoc newgs# :inventory #{}))})
            new-state (assoc-in game-state [:room-map :bedroom] new-bedroom)
            newer-state (go new-state "north")]
        (is (empty? (:inventory newer-state)))))))
