(ns advenjure.eval
  (:require [cljs.js]))

(defn eval [form] (cljs.js/eval
                    (cljs.js/empty-state)
                    form
                    {:eval cljs.js/js-eval
                     :source-map true
                     :context :expr}
                    :value))
