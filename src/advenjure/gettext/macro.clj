(ns advenjure.gettext.macro
  (:require [carica.core :as carica]))

(defmacro config [kw]
  (str (carica/config kw)))
