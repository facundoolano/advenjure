(ns advenjure.verbs-test
  (:require [clojure.test :refer :all]
            [advenjure.test-utils :refer :all]
            [advenjure.verbs :as verbs]
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

(def living (room/make "Living" "short description of living room"
                       :synonyms ["Living room"]
                       :initial-description "long description of living room"
                       :items #{(it/make ["sofa"] "just a sofa")}
                       :south :bedroom))

(def game-state {:current-room :bedroom
                 :out          ""
                 :room-map     (-> {:bedroom bedroom, :living living}
                                   (room/connect :bedroom :north :living))
                 :inventory    #{magazine}})

(deftest look-verb
  (let [look (get-verb "look")]
    (testing "look at room"
      (is-output (look game-state)
                 ["short description of bedroom"
                  "There was a bed there."
                  "There was a sock there."
                  "There was a drawer there. The drawer contained a pencil"
                  ""
                  "North: ???"]))))

(deftest look-at-verb
  (let [look-at (get-verb "look at")]
    (testing "look at inventory item"
      (is-output (look-at game-state "magazine")
                 "The cover reads 'Sports Almanac 1950-2000'")
      (is-output (look-at game-state "sports magazine")
                 "The cover reads 'Sports Almanac 1950-2000'"))

    (testing "look at room item"
      (is-output (look-at game-state "bed") "just a bed"))

    (testing "look at inventory container item"
      (let [coin      {:names ["coin"] :description "a nickle"}
            sack      {:names ["sack"] :items #{coin}}
            new-state (assoc game-state :inventory #{sack})]
        (is-output (look-at new-state "coin") "a nickle")))

    (testing "look at room container item"
      (is-output (look-at game-state "pencil") "it's a pencil"))

    (testing "look at closed container item"
      (let [coin      {:names ["coin"] :description "a nickle"}
            sack      {:names ["sack"] :items #{coin} :closed true}
            new-state (assoc game-state :inventory #{sack})]
        (is-output (look-at new-state "coin") "I didn't see that.")))

    (testing "look at ambiguous item name"
      (let [red-shoe   (it/make ["shoe" "red shoe"] "it's red")
            brown-shoe (it/make ["shoe" "brown shoe"] "an old brown shoe")
            new-state  (assoc game-state :inventory #{red-shoe brown-shoe})]
        (is-output (look-at new-state "shoe")
                   "Which shoe? The brown shoe or the red shoe?")
        (is-output (look-at new-state "brown shoe")
                   "an old brown shoe"))
      (let [red-shoe   (it/make ["shoe" "red shoe"] "it's red")
            brown-shoe (it/make ["shoe" "brown shoe"] "an old brown shoe")
            green-shoe (it/make ["shoe" "green shoe"] "an old green shoe")
            new-state  (assoc game-state :inventory #{red-shoe brown-shoe green-shoe})]
        (is-output (look-at new-state "shoe")
                   "Which shoe? The brown shoe, the green shoe or the red shoe?")
        (is-output (look-at new-state "brown shoe")
                   "an old brown shoe")))

    (testing "look at missing item"
      (is-output (look-at game-state "sofa") "I didn't see that."))

    (testing "look at container still describes"
      (is-output (look-at game-state "drawer")
                 "it's an open drawer."))))

(deftest look-inside-verb
  (let [look-inside (get-verb "look inside")]
    (testing "look in container lists contents"
      (is-output (look-inside game-state "drawer")
                 ["The drawer contained a pencil"]))

    (testing "look in container inside container"
      (let [bottle    {:names ["bottle"] :items #{{:names ["amount of water"]}}}
            sack      {:names ["sack"] :items #{bottle}}
            new-state (assoc game-state :inventory #{sack})]
        (is-output (look-inside new-state "bottle")
                   ["The bottle contained an amount of water"])))

    (testing "look inside non-container"
      (is-output (look-inside game-state "bed")
                 "I couldn't look inside a bed."))))

(deftest go-verb
  (let [go_       (get-verb "go")
        new-state (go_ game-state "north")]
    (testing "go to an unvisited room"
      (is-output new-state ["long description of living room"
                            "There was a sofa there."])
      (is (= (:current-room new-state) :living))
      (is (get-in new-state [:room-map :living :visited])))

    (testing "go to an already visited room"
      (let [newer-state (go_ new-state "south")]
        (is-output newer-state
                   ["short description of bedroom"
                    "There was a bed there."
                    "There was a sock there."
                    "There was a drawer there. The drawer contained a pencil"])
        (is (= (:current-room newer-state) :bedroom))
        (is (get-in newer-state [:room-map :bedroom :visited]))))

    (testing "go to a visited room name"
      (let [newer-state (go_ new-state "bedroom")]
        (is-output newer-state
                   ["short description of bedroom"
                    "There was a bed there."
                    "There was a sock there."
                    "There was a drawer there. The drawer contained a pencil"])
        (is (= (:current-room newer-state) :bedroom))
        (is (get-in newer-state [:room-map :bedroom :visited]))))

    (testing "go to a blocked direction"
      (is-output (go_ game-state "west") "couldn't go in that direction."))

    (testing "go to an invalid direction"
      (is-output (go_ game-state nil) "Go where?")
      (is-output (go_ game-state "crazy") "Go where?"))))

(deftest look-to-verb
  (let [look-to (get-verb "look to")]
    (testing "Look to a known direction"
      (let [new-state (assoc-in game-state [:room-map :living :known] true)]
        (is-output (look-to new-state "north") "The Living was in that direction.")))

    (testing "Look to a visited direction"
      (-> game-state
          (assoc-in [:room-map :living :visited] true)
          (look-to "north")
          (is-output "The Living was in that direction.")))

    (testing "Look to an unknown direction"
      (is-output (look-to game-state "north")
                 "I didn't know what was in that direction."))

    (testing "Look to a blocked direction"
      (-> game-state
          (assoc-in [:room-map :bedroom :north] "The door was on fire")
          (look-to "north")
          (is-output "That direction was blocked.")))

    (testing "Look to a valid direction where there's nothing"
      (is-output (look-to game-state "southwest")
                 "There was nothing in that direction."))

    (testing "Look to an invalid direction"
      (is-output (look-to game-state "noplace") "Look to where?"))

    (testing "Look to a room name"
      (-> game-state
          (assoc-in [:room-map :living :visited] true)
          (look-to "Living")
          (is-output "The Living was toward north.")))

    (testing "Look to a secondary room name"
      (-> game-state
          (assoc-in [:room-map :living :visited] true)
          (look-to "living room")
          (is-output ["The Living was toward north."])))))

(deftest go-back-verb
  (testing "Should remember previous room and go back"
    (let [go_       (get-verb "go")
          go-back   (get-verb "go back")
          new-state (-> game-state
                        (go_ "north")
                        (go-back))]
      (is (:current-room new-state) :bedroom)
      (is (:previous-room new-state) :living)
      (is-output new-state
                 ["long description of living room"
                  "There was a sofa there."
                  "short description of bedroom"
                  "There was a bed there."
                  "There was a sock there."
                  "There was a drawer there. The drawer contained a pencil"])))

  (testing "Should say can't go back if not known previous location")

  (testing "Should say can't go back if previous room not currently accesible"))

(deftest take-verb
  (let [take_ (get-verb "take")]
    (testing "take an item from the room"
      (let [new-state (take_ game-state "sock")]
        (is (it/get-from (:inventory new-state) "sock"))
        (is (empty? (it/get-from (:items (current-room new-state)) "sock")))
        (is-output new-state "Taken.")))

    (testing "take an item that's not takable"
      (let [new-state (take_ game-state "bed")]
        (is (= (:inventory game-state) (:inventory new-state)))
        (is-output new-state "I couldn't take that.")))

    (testing "take an item from inventory"
      (let [new-state (take_ game-state "magazine")]
        (is (= (:inventory game-state) (:inventory new-state)))
        (is-output new-state "I already had that.")))

    (testing "take an invalid item"
      (let [new-state (take_ game-state "microwave")]
        (is (= (:inventory game-state) (:inventory new-state)))
        (is-output new-state "I didn't see that.")))

    (testing "take with no parameter"
      (let [new-state (take_ game-state)]
        (is (= (:inventory game-state) (:inventory new-state)))
        (is-output new-state "Take what?")))

    (testing "take an item from other room"
      (let [new-state   (assoc game-state :current-room :living)
            newer-state (take_ new-state "sock")]
        (is (= (:inventory new-state) (:inventory newer-state)))
        (is-output newer-state "I didn't see that.")))

    (testing "take item in room container"
      (let [new-state (take_ game-state "pencil")]
        (is (it/get-from (:inventory new-state) "pencil"))
        (is (empty? (it/get-from (:items (current-room new-state)) "pencil")))
        (is-output new-state "Taken.")))

    (testing "take item in inv container"
      (let [coin        {:names ["coin"] :description "a nickle" :take true}
            sack        {:names ["sack"] :items #{coin}}
            new-state   (assoc game-state :inventory #{sack})
            newer-state (take_ new-state "coin")
            inv         (:inventory newer-state)
            new-sack    (it/get-from inv "sack")]
        (is (contains? inv coin))
        (is (not (contains? (:items new-sack) coin)))
        (is-output newer-state "Taken.")))

    (testing "take item in closed container"
      (let [coin        {:names ["coin"] :description "a nickle" :take true}
            sack        {:names ["sack"] :items #{coin} :closed true}
            new-state   (assoc game-state :inventory #{sack})
            newer-state (take_ new-state "coin")]
        (is (= (:inventory new-state) (:inventory newer-state)))
        (is-output newer-state "I didn't see that.")))))

(deftest take-all-verb
  (let [take-all (get-verb "take all")]
    (testing "take all items in the room with containers, ignore inventory"
      (let [new-state  (take-all game-state)
            item-names (set (map #(first (:names %)) (:inventory new-state)))]
        (is (= item-names #{"magazine" "pencil" "sock"}))
        ;; lousy, assumes some order in items
        (is-output new-state ["Sock: Taken."
                              "Pencil: Taken."])))

    (testing "attempt taking items that define :take property"
      (let [new-bedroom (-> bedroom
                            (room/add-item (it/make "shoe" "a shoe" :take "I didn't want that."))
                            (room/add-item (it/make "fridge" "a fridge" :take false)))
            new-state   (-> game-state
                            (assoc-in [:room-map :bedroom] new-bedroom)
                            (take-all))
            item-names  (set (map #(first (:names %)) (:inventory new-state)))]
        (is (= item-names #{"magazine" "pencil" "sock"}))
        ;; lousy, assumes some order in items
        (is-output new-state ["Sock: Taken."
                              "Shoe: I didn't want that."
                              "Fridge: I couldn't take that."
                              "Pencil: Taken."])))

    (testing "take all when no items left"
      (let [empty-state (assoc-in game-state [:room-map :bedroom :items] #{})
            new-state   (take-all empty-state)]
        (is-output new-state "I saw nothing worth taking.")
        (is (= (:inventory game-state) (:inventory new-state)))))))

(deftest open-verb
  (let [open (get-verb "open")]
    (testing "open a closed item"
      (let [sack        (it/make ["sack"] "a sack" :items #{} :closed true)
            new-state   (assoc game-state :inventory #{sack})
            newer-state (open new-state "sack")
            new-sack    (it/get-from (:inventory newer-state) "sack")]
        (is-output newer-state "The sack was empty.")
        (is (not (:closed new-sack)))))

    (testing "open an already open item"
      (let [sack        (it/make ["sack"] "a sack" :items #{} :closed false)
            new-state   (assoc game-state :inventory #{sack})
            newer-state (open new-state "sack")]
        (is-output newer-state "It was already open.")
        (is-same new-state newer-state)))

    (testing "open a non openable item"
      (let [sack        (it/make ["sack"] "a sack" :items #{})
            new-state   (assoc game-state :inventory #{sack})
            newer-state (open new-state "sack")]
        (is-output newer-state "I couldn't open that.")
        (is-same new-state newer-state)))

    (testing "open a missing item"
      (let [new-state (open game-state "sack")]
        (is-output new-state "I didn't see that.")
        (is-same game-state new-state)))

    (testing "open a container inside a container"
      (let [bottle      (it/make ["bottle"] "a bottle" :closed true
                                 :items #{(it/make "amount of water")})
            sack        (it/make ["sack"] "a sack" :items #{bottle})
            new-state   (assoc game-state :inventory #{sack})
            newer-state (open new-state "bottle")
            new-sack    (it/get-from (:inventory newer-state) "sack")
            new-bottle  (it/get-from (:items new-sack) "bottle")]
        (is-output newer-state "The bottle contained an amount of water")
        (is (not (:closed new-bottle)))))))

(deftest close-verb
  (let [close (get-verb "close")]
    (testing "close an open item"
      (let [sack        (it/make ["sack"] "a sack" :items #{} :closed false)
            new-state   (assoc game-state :inventory #{sack})
            newer-state (close new-state "sack")
            new-sack    (first (it/get-from (:inventory newer-state) "sack"))]
        (is-output newer-state "Closed.")
        (is (:closed new-sack))))

    (testing "close an already closed item"
      (let [sack        (it/make ["sack"] "a sack" :items #{} :closed true)
            new-state   (assoc game-state :inventory #{sack})
            newer-state (close new-state "sack")]
        (is-output newer-state "It was already closed.")
        (is-same new-state newer-state)))

    (testing "close a non openable item"
      (let [sack        (it/make ["sack"] "a sack" :items #{})
            new-state   (assoc game-state :inventory #{sack})
            newer-state (close new-state "sack")]
        (is-output newer-state "I couldn't close that.")
        (is-same new-state newer-state)))

    (testing "close a missing item"
      (let [new-state (close game-state "sack")]
        (is-output new-state "I didn't see that.")
        (is-same game-state new-state)))

    (testing "close a container inside a container"
      (let [bottle      (it/make ["bottle"] "a bottle " :closed false
                                 :items #{(it/make "amount of water")})
            sack        (it/make ["sack"] "a sack" :items #{bottle})
            new-state   (assoc game-state :inventory #{sack})
            newer-state (close new-state "bottle")
            new-sack    (first (it/get-from (:inventory newer-state) "sack"))
            new-bottle  (first (it/get-from (:items new-sack) "bottle"))]
        (is-output newer-state "Closed.")
        (is (:closed new-bottle))))))

(deftest unlock-verb
  (let [open      (get-verb "open")
        unlock    (get-verb "unlock (?<item1>.*) with")
        chest     (it/make ["chest"] "a treasure chest" :closed true :locked true)
        ckey      (it/make ["key"] "the chest key" :unlocks chest)
        other-key (it/make ["other key"] "another key" :unlocks drawer)
        inventory (conj #{} chest ckey other-key)
        new-state (assoc game-state :inventory inventory)]

    (testing "open a locked item"
      (let [newer-state (open new-state "chest")]
        (is-output newer-state "It was locked.")
        (is-same new-state newer-state)))

    (testing "unlock a locked item"
      (let [newer-state (unlock new-state "chest" "key")
            new-chest   (it/get-from (:inventory newer-state) "chest")]
        (is-output newer-state "Unlocked.")
        (is (not (:locked new-chest)))
        (is-output (open newer-state "chest") "Opened.")))

    (testing "unlock an already unlocked item"
      (let [newer-state (unlock new-state "chest" "key")
            last-state  (unlock newer-state "chest" "other key")]
        (is-output last-state "It wasn't locked.")
        (is-same newer-state last-state)))

    (testing "unlock what?"
      (let [newer-state (unlock new-state)]
        (is-output newer-state "Unlock what?")
        (is-same new-state newer-state)))

    (testing "unlock with what?"
      (let [newer-state (unlock new-state "chest")]
        (is-output newer-state "Unlock chest with what?")
        (is-same new-state newer-state)))

    (testing "unlock a non lockable item"
      (let [newer-state (unlock new-state "drawer" "key")]
        (is-output newer-state "I couldn't unlock that.")
        (is-same new-state newer-state)))

    (testing "unlock with item that didn't unlock"
      (let [newer-state (unlock new-state "chest" "sock")]
        (is-output newer-state "That didn't work.")
        (is-same new-state newer-state)))

    (testing "unlock with item that unlocks another thing"
      (let [newer-state (unlock new-state "chest" "other key")]
        (is-output newer-state "That didn't work.")
        (is-same new-state newer-state)))))

(deftest read-verb
  (let [read_ (get-verb "read")]
    (testing "Read a readble item"
      (let [new-state (read_ game-state "magazine")]
        (is-output new-state
                   "Tells the results of every major sports event till the end of the century.")
        (is (= game-state (assoc new-state :out "")))))

    (testing "Read a non readble item"
      (let [new-state (read_ game-state "sock")]
        (is-output new-state"I couldn't read that.")
        (is (= game-state (assoc new-state :out "")))))))

(deftest inventory-verb
  (let [inventory (get-verb "inventory")]
    (testing "list inventory contents"
      (is-output (inventory game-state) ["I was carrying:" "A magazine"]))

    (testing "empty inventory"
      (let [new-state (assoc game-state :inventory #{})]
        (is-output (inventory new-state) "I wasn't carrying anything.")))

    (testing "list inventory with container"
      (let [bottle {:names ["bottle"] :items #{{:names ["amount of water"]}}}
            sack   {:names ["sack"] :items #{bottle}}]
        (-> (assoc game-state :inventory #{sack})
            (inventory)
            (is-output ["I was carrying:"
                        "A sack. The sack contained a bottle"]))))))

(deftest pre-post-conditions
  (let [take_   (get-verb "take")
        look-at (get-verb "look at")
        open    (get-verb "open")
        go_     (get-verb "go")
        unlock  (get-verb "unlock (?<item1>.*) with")]

    (testing "Override couldn't take message"
      (let [new-drawer  (assoc drawer :take "It's too heavy to take.")
            new-bedroom (assoc bedroom :items #{new-drawer})
            new-state   (assoc-in game-state [:room-map :bedroom] new-bedroom)
            newer-state (take_ new-state "drawer")]
        (is (= new-state (assoc newer-state :out "")))
        (is-output newer-state "It's too heavy to take.")))

    (testing "Override look at description"
      (let [
            new-magazine  (assoc magazine :look-at "I didn't want to look at it.")
            new-inventory (it/replace-from (:inventory game-state) magazine new-magazine)
            new-state     (assoc game-state :inventory new-inventory)
            newer-state   (look-at new-state "magazine")]
        (is (= new-state (assoc newer-state :out "")))
        (is-output newer-state "I didn't want to look at it.")))

    (testing "precondition returns false"
      (let [sock2       (it/make ["other sock"] "another sock"
                                 :take `#(contains? (:inventory %) sock))
            new-state   (assoc-in game-state [:room-map :bedroom]
                                  (room/add-item bedroom sock2))
            newer-state (take_ new-state "other sock")]
        (is (= new-state (assoc newer-state :out "")))
        (is-output newer-state "I couldn't take that.")))

    (testing "precondition returns error message"
      (let [sock2       (it/make ["other sock"] "another sock"
                                 :take `#(or (contains? (:inventory %) sock)
                                             "Not unless I have the other sock."))
            new-state   (assoc-in game-state [:room-map :bedroom]
                                  (room/add-item bedroom sock2))
            newer-state (take_ new-state "other sock")]
        (is (= new-state (assoc newer-state :out "")))
        (is-output newer-state "Not unless I have the other sock.")))

    (testing "precondition other syntax"
      (let [sock2       (it/make ["other sock"] "another sock"
                                 :take {:pre `#(or (contains? (:inventory %) sock)
                                                   "Not unless I have the other sock.")})
            new-state   (assoc-in game-state [:room-map :bedroom]
                                  (room/add-item bedroom sock2))
            newer-state (take_ new-state "other sock")]
        (is (= new-state (assoc newer-state :out "")))
        (is-output newer-state "Not unless I have the other sock.")))

    (testing "precondition returns true"
      (let [sock2 (it/make ["other sock"] "another sock"
                           :take `#(or (contains? (:inventory %) sock)
                                       "Not unless I have the other sock."))]
        (-> game-state
            (assoc-in [:room-map :bedroom] (room/add-item bedroom sock2))
            (update :inventory conj sock)
            (take_ "other sock")
            (is-output "Taken."))))

    (testing "precondition for compound verb"
      (let [beer  (it/make ["beer"] "a beer")
            chest (it/make ["chest"] "a treasure chest" :closed true :locked true
                           :unlock `(fn [gs# the-key#]
                                      (or (contains? (:inventory gs#) ~beer)
                                          "Only if I have a beer.")))
            ckey  (it/make ["key"] "the chest key" :unlocks chest)]
        (-> game-state
            (assoc :inventory #{chest ckey})
            (unlock "chest" "key")
            (is-output "Only if I have a beer.")
            (assoc :inventory #{chest ckey beer})
            (unlock "chest" "key")
            (is-output "Unlocked."))))

    (testing "override message for go"
      (let [new-bedroom (assoc bedroom :south "No way I was going south.")
            new-state   (assoc-in game-state [:room-map :bedroom] new-bedroom)
            newer-state (go_ new-state "south")]
        (is (= new-state (assoc newer-state :out "")))
        (is-output newer-state "No way I was going south.")))

    (testing "precondition for go"
      (let [wallet      (it/make "wallet")
            new-bedroom (assoc bedroom :north `#(if (contains? (:inventory %) ~wallet)
                                                  :living
                                                  "couldn't leave without my wallet."))
            new-state   (assoc-in game-state [:room-map :bedroom] new-bedroom)]
        (is-output (go_ new-state "north") "couldn't leave without my wallet.")
        (let [newer-state (assoc new-state :inventory #{wallet})
              last-state  (go_ newer-state "north")]
          (is (= :living (:current-room last-state))))))

    (testing "postcondition replace object"
      (let [broken-bottle (it/make "broken bottle")]
        (defn break-bottle [oldgs newgs]
          (let [inventory (:inventory newgs)
                bottle    (first (it/get-from inventory "bottle"))
                new-inv   (it/replace-from inventory bottle broken-bottle)]
            (-> newgs
                (say "I think I broke it.")
                (assoc :inventory new-inv))))

        (let [bottle      (it/make ["bottle"] "a bottle"
                                   :open {:post `break-bottle})
              new-state   (assoc game-state :inventory #{bottle})
              newer-state (open new-state "bottle")]
          (is-output newer-state "I think I broke it.")
          (is (contains? (:inventory newer-state) broken-bottle)))))

    (testing "postcondition for compound"
      (def beer (it/make ["beer"] "a beer"))
      (defn get-beer [oldgs newgs]
        (-> newgs
            (say "There was a beer inside. Taking it.")
            (update :inventory conj beer)))

      (let [chest       (it/make ["chest"] "a treasure chest" :closed true :locked true
                                 :unlock {:post `get-beer})
            ckey        (it/make ["key"] "the chest key" :unlocks chest)
            new-state   (assoc game-state :inventory #{chest ckey})
            newer-state (unlock new-state "chest" "key")]
        (is-output newer-state ["Unlocked." "There was a beer inside. Taking it."])
        (is (contains? (:inventory newer-state) beer))))

    (testing "postcondition for go"
      (let [new-bedroom (assoc bedroom :north {:pre  :living
                                               :post `(fn [oldgs# newgs#] ;empties inv
                                                        (assoc newgs# :inventory #{}))})
            new-state   (assoc-in game-state [:room-map :bedroom] new-bedroom)
            newer-state (go_ new-state "north")]
        (is (empty? (:inventory newer-state)))))))
