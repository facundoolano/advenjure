(ns advenjure.ui.request
  (:require [clojure.data.json :as json]))

; FIXME handle errors
(defn request
  ([url] (request url {}))
  ([url params]
   (let [json? (= :json (:response-format params))
         text (try (slurp url) (catch Exception e (if json? "{}" "")))]
    (if json? (json/read-str text) text))))

