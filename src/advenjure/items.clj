(ns advenjure.items
  (:require [clojure.string :as string]))

(defrecord Item [names description])

(defn open-defaults
  "If either closed, close or open keywords are in the item, set the defaults
  for an openable item."
  [item]
  (if (or (:open item) (:close item) (contains? item :closed))
    {:open true :close true :closed true}))

(defn unlock-defaults
  [item]
  (if (or (:unlock item) (contains? item :locked))
    {:unlock true :locked true :close true :closed true :open true}))

(defn talk-defaults [item] (if (:dialog item) {:talk true}))

(defn make
  ([names description & {:as extras}]
   (let [names (if (string? names) [names] names)]
     (map->Item (merge {:names names :description description}
                       (open-defaults extras)
                       (unlock-defaults extras)
                       (talk-defaults extras)
                       extras))))
  ([names]
   (make names "There's nothing special about it.")))

(defn iname
  "Get the first name of the item."
  [item] (first (:names item)))

(declare describe-container)

(defn print-list-item [item]
  (let [vowel? (set "aeiouAEIOU")]
    (str
     (if (vowel? (first (iname item))) "an " "a ")
     (iname item))))

(defn ntabs [n] (apply str (repeat n "  ")))

(defn print-list
  ([items] (print-list items 1))
  ([items level]
   (string/join
     (str "\n" (ntabs level))
     (for [item items]
       (str (string/capitalize (print-list-item item))
            (describe-container item ". " (inc level)))))))

(defn describe-container
  "Recursively lists the contents of the item. If a prefix is given, it will be
  appended to the resulting string.
  If the item is empty or closed, the message will say so.
  If the item is not a container, returns nil."
  ([container] (describe-container container "" 1))
  ([container prefix] (describe-container container prefix 1))
  ([container prefix level]
   (if-let [items (:items container)]
     (cond
       (:closed container) (str prefix "The " (iname container) " is closed.")
       (empty? items) (str prefix "The " (iname container) " is empty.")
       :else (str prefix "The " (iname container)
                  " contains:\n" (ntabs level) (print-list items))))))

(defn visible-items
  "Return items inside a container only if not closed."
  [container]
  (if (:closed container)
    #{}
    (:items container)))


(defn get-from
  "Get the spec for the item with the given name, if it's in the given set,
  or is contained by one of its items."
  ; TODO allow multiple results --i.e. door --> "glass door" "wooden door"
  [item-set item-name]
  (or (first (filter #(some #{item-name} (:names %)) item-set))
      (first (map #(get-from (:items %) item-name)
                  (filter #(not-empty (visible-items %)) item-set)))))

(defn remove-from
  "Try to -recursively- remove the item from the given set. It takes a full
  item, not a name. Return the new state of the set, if the item is not found,
  return the set unmodified."
  [item-set item-spec]
  (if (contains? item-set item-spec)
    (disj item-set item-spec)
    (set (map (fn [item] (if-let [inner (:items item)]
                           (assoc item :items (remove-from inner item-spec))
                           item))
              item-set))))

(defn replace-from
  "Look for the old-item in the given set, remove it and put the new-item in the same place.
  Return the new state of the set."
  [item-set old-item new-item]
  ; TODO try to avoid copy paste the method above
  (if (contains? item-set old-item)
    (conj (disj item-set old-item) new-item)
    (set (map (fn [item] (if-let [inner (:items item)]
                           (assoc item :items (replace-from inner old-item new-item))
                           item))
              item-set))))

(defn all-item-names
  [item-set]
  (if (nil? item-set)
    []
    (let [this-names (flatten (map :names item-set))
          inner-names (flatten (map #(all-item-names (visible-items %))
                                    item-set))]
      (concat this-names inner-names))))
