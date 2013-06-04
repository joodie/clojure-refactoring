(ns clojure-refactoring.t-extract-method
  (:use midje.sweet
        clojure-refactoring.extract-method
        clojure-refactoring.support.find-bindings-above-node)
  (:require [clojure-refactoring.ast :as ast]
            [clojure-refactoring.support.parser :as parser]))

(defn fn-call-sexp [sexp]
  (read-string (ast/ast->string (fn-call (ast/sexp->parsley sexp)))))

(defn fn-name-sexp [sexp]
  (symbol (first (:content (fn-name (ast/sexp->parsley sexp))))))

(defn remove-extracted-function-sexp [extracted toplevel new-fn]
  (ast/ast->string
   (call-extracted
    (parser/parse1 extracted)
    (parser/parse1 toplevel)
    (parser/parse1 new-fn))))


(fact "should return the name of a function as a parsley node"
  (fn-name-sexp '(defn a [c] c)) => 'a)

(fact "should use the arguments of a function node to return a call to it."
  (fn-call-sexp '(defn a [b] (+ 1 2))) => '(a b))


(fact "should remove extracted expression in original toplevel function"
  (remove-extracted-function-sexp
   "(inc a)"
   "(defn b [a] (inc a))"
   "(defn arr [a] (inc a))") => "(defn b [a] (arr a))")

;; Acceptance level testing below
(fact "should use vars from let as arguments"
  (extract-method
   "(defn add [s]\n(let [a 1] (+ a 1)))"
   "(+ a 1)"
   "add-number") =>
  "(defn add-number
  [a]
  (+ a 1))\n\n(defn add [s]\n(let [a 1] (add-number a)))")

(fact "should not use bindings more than once"
  (extract-method
   "(defn a [s] (if (.contains s \",\") 1 s))"
   "(if (.contains s \",\") 1 s)"
   "b") =>
  "(defn b\n  [s]\n  (if (.contains s \",\") 1 s))\n\n(defn a [s] (b s))")


(fact "should work in list comprehensions"
  (extract-method
   "(defn add [s]\n(for [x (re-split #\",\" s)] (Integer. x)))"
   "(Integer. x)"
   "to-i") =>
   "(defn to-i
  [x]
  (Integer. x))\n\n(defn add [s]\n(for [x (re-split #\",\" s)] (to-i x)))"
  (extract-method
   "(defn add [s]\n(for [x (re-split #\",\" s)] (Integer. x)))"
   "(re-split #\",\" s)"
   "split-string") =>
   "(defn split-string
  [s]
  (re-split #\",\" s))\n\n(defn add [s]\n(for [x (split-string s)] (Integer. x)))" )
