(ns advenjure.async
  #?(:cljs (:require-macros [cljs.core.async.macros]))
  #?(:cljs (:require [cljs.core.async]
                     [cljs.core.async.impl.protocols])))

(defn cljs-env?
  "
  Take the &env from a macro, and tell whether we are expanding into cljs.
  "
  [env]
  (boolean (:ns env)))

(defmacro if-cljs
  "
  Return then if we are generating cljs code and else for Clojure code.
  https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ
  "
  [then else]
  (if (cljs-env? &env) then else))


(defmacro <!?
  "
  If value is a channel (implements ReadPort protocol), take the value from it
  (<!), otherwise return as is.
  "
  [value]
  `(if-cljs
      (let [result# ~value]
        (if (satisfies? cljs.core.async.impl.protocols/ReadPort result#)
          (cljs.core.async/<! result#)
          result#))
      ~value))


(defmacro aloop
  "
  replace with go-loop in cljs and with loop in clj.
  "
  [& expr]
  `(if-cljs (cljs.core.async.macros/go-loop ~@expr)
            (loop ~@expr)))


