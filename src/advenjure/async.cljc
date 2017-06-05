(ns advenjure.async
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require #?(:clj [clojure.core.async :refer [go <!]]
               :cljs [cljs.core.async :refer [<!]])
            #?(:clj [clojure.core.async.impl.protocols :refer [ReadPort]]
               :cljs [cljs.core.async.impl.protocols :refer [ReadPort]])))

(defn force-chan [value]
  (go
    (if (satisfies? ReadPort value) (<! value) value)))
