(ns advenjure.hooks-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer [<!!]]
            [advenjure.test-utils :refer :all]
            [advenjure.game :refer :all]
            [advenjure.items :as it]
            [advenjure.rooms :as room]))

(def magazine (it/make ["magazine" "sports magazine"]
                       "The cover reads 'Sports Almanac 1950-2000'"
                       :take true
                       :read "Tells the results of every major sports event till the end of the century."))

(def chest (it/make ["chest"] "a treasure chest" :closed true :locked true))
(def ckey (it/make ["key"] "the chest key" :unlocks chest))

(def bedroom (room/make "Bedroom" "short description of bedroom"
                        :initial-description "long description of bedroom"
                        :items #{chest (it/make ["bed"] "just a bed")}
                        :north :living
                        :visited true))

(def living (room/make "Bedroom" "short description of living room"
                       :initial-description "long description of living room"
                       :items #{(it/make ["sofa"] "just a sofa")}
                       :south :bedroom))

(def base-state (-> (make {:bedroom bedroom, :living living} :bedroom #{magazine ckey})
                    (assoc :counter 0)))

(deftest before-room-test
  (testing "modify the game state before changing room"
    (let [hook        (fn [gs] (-> gs (update-in [:counter] inc)
                                   (assoc :called-in (:current-room gs))
                                   (assoc :already-visited (get-in gs [:room-map :living :visited]))))
          with-config (use-plugin base-state {:hooks {:before-change-room hook}})
          after-look  (<!! (process-input with-config "look"))
          after-room  (<!! (process-input after-look "go north"))
          after-look2 (<!! (process-input after-room "look"))]
      (is (= 0 (:counter after-look)))
      (is (= nil (:called-in after-look)))
      (is (= 1 (:counter after-room)))
      (is (= :living (:called-in after-room)))
      (is (= false (:already-visited after-room)))
      (is (= 1 (:counter after-look2)))))

  (testing "pipe multiple hooks"
    (let [hook1      (fn [gs] (update-in gs [:counter] inc))
          hook2      (fn [gs] (assoc gs :second true))
          game-state (-> base-state
                         (use-plugin {:hooks {:before-change-room hook1}})
                         (use-plugin {:hooks {:before-change-room hook2}})
                         (process-input "look")
                         <!!
                         (process-input "go north")
                         <!!)]
      (is (= 1 (:counter game-state)))
      (is (= true (:second game-state))))))

(deftest after-room-test
  (testing "modify the game state after changing room"
    (let [hook        (fn [gs] (-> gs (update-in [:counter] inc)
                                   (assoc :called-in (:current-room gs))
                                   (assoc :already-visited (get-in gs [:room-map :living :visited]))))
          with-config (use-plugin base-state {:hooks {:after-change-room hook}})
          after-look  (<!! (process-input with-config "look"))
          after-room  (<!! (process-input after-look "go north"))
          after-look2 (<!! (process-input after-room "look"))]
      (is (= 0 (:counter after-look)))
      (is (= nil (:called-in after-look)))
      (is (= 1 (:counter after-room)))
      (is (= :living (:called-in after-room)))
      (is (= true (:already-visited after-room))) ; main difference is room already set to visited
      (is (= 1 (:counter after-look2))))))

(deftest before-handler-test
  (testing "modify the game state before executing a verb handler"
    (let [hook        (fn [gs] (-> gs (update-in [:counter] inc)
                                   (assoc :called-in (:current-room gs))))
          with-config (use-plugin base-state {:hooks {:before-handler hook}})
          after-look  (<!! (process-input with-config "look"))
          after-room  (<!! (process-input after-look "go north"))
          after-look2 (<!! (process-input after-room "look"))]
      (is (= 1 (:counter after-look)))
      (is (= :bedroom (:called-in after-look)))
      (is (= 2 (:counter after-room)))
      (is (= :bedroom (:called-in after-room)))
      (is (= 3 (:counter after-look2))))))

(deftest after-handler-test
  (testing "modify the game state after executing a verb handler"
    (let [hook        (fn [gs] (-> gs (update-in [:counter] inc)
                                   (assoc :called-in (:current-room gs))))
          with-config (use-plugin base-state {:hooks {:after-handler hook}})
          after-look  (<!! (process-input with-config "look"))
          after-room  (<!! (process-input after-look "go north"))
          after-look2 (<!! (process-input after-room "look"))]
      (is (= 1 (:counter after-look)))
      (is (= :bedroom (:called-in after-look)))
      (is (= 2 (:counter after-room)))
      (is (= :living (:called-in after-room)))
      (is (= 3 (:counter after-look2))))))

(deftest before-item-test
  (testing "modify the game state before executing an item handler"
    (let [hook        (fn [gs kw & items]
                        (-> gs (update-in [:counter] inc)
                            (assoc :received kw)))
          with-config (use-plugin base-state {:hooks {:before-item-handler hook}})
          after-look  (<!! (process-input with-config "look"))
          after-look2 (<!! (process-input after-look "look at magazine"))]
      (is (= 0 (:counter after-look)))
      (is (= nil (:received after-look)))
      (is (= 1 (:counter after-look2)))
      (is (= :look-at (:received after-look2)))))

  (testing "dont call if precondition fails"
    (let [hook        (fn [gs kw & items]
                        (-> gs (update-in [:counter] inc)
                            (assoc :received kw)))
          with-config (use-plugin base-state {:hooks {:before-item-handler hook}})
          after-take  (<!! (process-input with-config "take bed"))]
      (is (= 0 (:counter after-take)))
      (is (= nil (:received after-take)))))

  (testing "modify the game state before executing a compound item handler"
    (let [hook         (fn [gs kw & items]
                         (-> gs (update-in [:counter] inc)
                             (assoc :received kw)))
          with-config  (use-plugin base-state {:hooks {:before-item-handler hook}})
          after-look   (<!! (process-input with-config "look"))
          after-unlock (<!! (process-input after-look "unlock chest with key"))]
      (is (= 0 (:counter after-look)))
      (is (= nil (:received after-look)))
      (is (= 1 (:counter after-unlock)))
      (is (= :unlock (:received after-unlock))))))

(deftest after-item-test
  (testing "modify the game state after executing an item handler"
    (let [hook        (fn [gs kw & items]
                        (-> gs (update-in [:counter] inc)
                            (assoc :received kw)))
          with-config (use-plugin base-state {:hooks {:after-item-handler hook}})
          after-look  (<!! (process-input with-config "look"))
          after-look2 (<!! (process-input after-look "look at magazine"))]
      (is (= 0 (:counter after-look)))
      (is (= nil (:received after-look)))
      (is (= 1 (:counter after-look2)))
      (is (= :look-at (:received after-look2)))))

  (testing "modify the game state after executing a compound item handler"
    (let [hook         (fn [gs kw & items]
                         (-> gs (update-in [:counter] inc)
                             (assoc :received kw)))
          with-config  (use-plugin base-state {:hooks {:after-item-handler hook}})
          after-look   (<!! (process-input with-config "look"))
          after-unlock (<!! (process-input after-look "unlock chest with key"))]
      (is (= 0 (:counter after-look)))
      (is (= nil (:received after-look)))
      (is (= 1 (:counter after-unlock)))
      (is (= :unlock (:received after-unlock))))))
