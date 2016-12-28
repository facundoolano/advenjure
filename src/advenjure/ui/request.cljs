(ns advenjure.ui.request
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [ajax.core :refer [GET]]
            [cljs.core.async :refer [>! chan]]))

(defn request
  ([url] (request url {}))
  ([url params]
   (go
     (let [res-chan (chan)
           handler (fn [result] (go (>! res-chan (or result ""))))]
      (GET url (merge params {:handler handler :error-handler handler}))
      res-chan))))

