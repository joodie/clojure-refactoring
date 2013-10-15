(ns clojure-refactoring.support.t-formatter
  (:use midje.sweet
        clojure-refactoring.support.formatter)
  (:require [clojure-refactoring.ast :as ast]))

(defn format-from-sexp [s]
  (->> (ast/sexp->parsley s)
       ast/strip-whitespace
       format-ast
       ast/ast->string))

(fact "should strip whitespaces"
  (ast/ast->string
   (ast/strip-whitespace
    (ast/sexp->parsley '(+ 1 2)))) =>"(+12)")

(fact "should add whitespace to a one line sexp"
  (format-from-sexp '(+ 1 2)) => "(+ 1 2)")

(fact "should format defn form correctly"
  (format-from-sexp '(defn w [a] (inc a))) => "(defn w [a]\n  (inc a))")

(fact "should format threading-last form"
  (format-from-sexp '(->> (inc 1) dec inc dec zero?)) => "(->> (inc 1)\n   dec\n   inc\n   dec\n   zero?)"
  (format-from-sexp '(->> (:a a) (map zero?) (filter foo?))) => "(->> (:a a)\n   (map zero?)\n   (filter foo?))"
  (format-from-sexp '(->> (map #(Integer. %) s) (reduce +))) => "(->> (map #(Integer. %) s)\n   (reduce +))")

(fact "should format threading-first form"
  (format-from-sexp '(-> (inc 1) dec inc dec zero?)) => "(-> (inc 1)\n   dec\n   inc\n   dec\n   zero?)"
  (format-from-sexp '(-> (:a a) (map zero?) (filter foo?))) => "(-> (:a a)\n   (map zero?)\n   (filter foo?))")

(fact "should format threaded inside another form"
  (format-from-sexp '(inc (-> (inc 1) dec))) => "(inc (-> (inc 1)\n   dec))")
