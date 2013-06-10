(ns clojure-refactoring.support.t-parsley-walk
  (:use midje.sweet
        clojure-refactoring.support.parsley-walk
        clojure-refactoring.destructuring)
  (:require [clojure-refactoring.ast :as ast]
            [clojure-refactoring.support.parser :as parser]))


(def root-ast (parser/parse1 "(defn a [b] (+ (b :foo) (b :bar)))"))
(def old-vec (ast/parsley-fn-args root-ast))


(fact "should return new arg list with each symbol in old-vec founded in binding map keys replaced by the corresponding value"
  (ast/ast->string
   (postwalk-replace (lookups-to-binding-map (find-lookups root-ast)) old-vec)) => "[{foo :foo bar :bar }]")
