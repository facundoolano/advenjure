(ns advenjure.async
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require #?(:clj [clojure.core.async :refer [go <!]]
               :cljs [cljs.core.async :refer [<!]])
            #?(:clj [clojure.core.async.impl.protocols :refer [ReadPort]]
               :cljs [cljs.core.async.impl.protocols :refer [ReadPort]])))


;; sadly it's too difficult to make sure we don't nest a channel in a channel
;; this will force the value to be exactly a valua inside one chan
(defn force-chan [value]
  (go
    (if (satisfies? ReadPort value)
      (-> value <! force-chan <!)
      value)))

(defn debug [value]
  (if (satisfies? ReadPort value) "PORT!" "value"))
