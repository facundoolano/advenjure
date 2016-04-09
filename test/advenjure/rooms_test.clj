(ns advenjure.rooms-test
  (:require [clojure.test :refer :all]
            [advenjure.rooms :refer :all]
            [advenjure.items :as it]))

(def bedroom (make "Bedroom" "this is a short bedroom description."
                   :initial-description "First time I see this."
                   :visited true))
(def living (make "Living room" "short description" :visited false))
(def kitchen (make "Kitchen" "this is a kitchen." :items #{(it/make "oven")}))
(def full-kitchen (make "Kitchen" "this is a kitchen." :items #{(it/make ["oven"] "an oven"
                                                                         :items #{(it/make ["turkey"] "a turkey")})}))

(deftest describe-room-test
  (testing "describe visited"
    (is (= (describe bedroom)
           "this is a short bedroom description.")))

  (testing "describe not visited initial"
    (is (= (describe (assoc bedroom :visited false))
           "First time I see this.")))

  (testing "describe not visited initial defaults"
    (is (= (describe living) "short description")))

  (testing "describe list items"
    (is (= (describe kitchen)
           "this is a kitchen.\nThere's an oven here.")))
  (testing "describe list container contents"
    (is (= (describe full-kitchen)
           "this is a kitchen.\nThere's an oven here. The oven contains a turkey"))

    (testing "describe override item mention"
      (is (= (describe (assoc kitchen
                              :description "This is a kitchen with an open oven."
                              :item-descriptions {"oven" ""}))
             "This is a kitchen with an open oven."))
      (is (= (describe (assoc kitchen
                              :description "This is a kitchen."
                              :item-descriptions {"oven" "In the corner is an open oven"}))
             "This is a kitchen. In the corner is an open oven")))

    (testing "describe override removed item"
      (is (= (describe (assoc kitchen
                              :description "This is a kitchen."
                              :item-descriptions {"microwave" "In the corner is a microwave"}))
             "This is a kitchen.\nThere's an oven here.")))))
