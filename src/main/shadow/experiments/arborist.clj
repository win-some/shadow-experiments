(ns shadow.experiments.arborist
  (:require
    [shadow.experiments.arborist.fragments :as fragments]
    [shadow.experiments.arborist.components :as comp]))

(defmacro << [& body]
  (fragments/make-fragment &env body))

;; I prefer << but <> looks more familiar to reagent :<>
;; costs nothing to have both, let the user decide
(defmacro <> [& body]
  (fragments/make-fragment &env body))

(defmacro fragment [& body]
  (fragments/make-fragment &env body))

(defmacro html [& body]
  (fragments/make-fragment &env body))

(defmacro svg [& body]
  (fragments/make-fragment (assoc &env ::fragments/svg true) body))

;; only here for bench-fragment, do not use directly.
(defmacro fragment-fallback [& body]
  (fragments/make-fragment (assoc &env ::fragments/optimize false) body))

(defmacro defc [& args]
  `(comp/defc ~@args))