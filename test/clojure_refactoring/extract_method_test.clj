(ns clojure-refactoring.extract-method-test
  (:use clojure-refactoring.extract-method :reload)
  (:use clojure.test
        clojure-refactoring.support.find-bindings-above-node)
  (:require [clojure-refactoring.ast :as ast])
  (:require [clojure-refactoring.support.parser :as parser]))

(use-fixtures :once #(time (%)))

(defn fn-call-sexp [sexp]
  (read-string (ast/ast->string (fn-call (ast/sexp->parsley sexp)))))

(defn fn-name-sexp [sexp]
         (symbol (first (:content (fn-name (ast/sexp->parsley sexp))))))

(deftest fn_name
  (is (= (fn-name-sexp '(defn a [c] c)) 'a)))

(deftest fn_call
  (is (= (fn-call-sexp '(defn a [b] (+ 1 2))) '(a b))))

(defn remove-extracted-function-sexp [extracted toplevel new-fn]
  (ast/ast->string
   (call-extracted
    (parser/parse1 extracted)
    (parser/parse1 toplevel)
    (parser/parse1 new-fn))))

(deftest remove_extracted_function
  (is (= (remove-extracted-function-sexp
          "(inc a)"
          "(defn b [a] (inc a))"
          "(defn arr [a] (inc a))")
         "(defn b [a] (arr a))")))

;; Acceptance level testing below
(deftest uses_variables_from_let
  (is (= (extract-method
          "(defn add [s]
(let [a 1] (+ a 1)))" "(+ a 1)" "add-number")
"(defn add-number
  [a]
  (+ a 1))

(defn add [s]
(let [a 1] (add-number a)))")))


(deftest works_in_list_comprehensions
	(is (= (extract-method
                "(defn add [s]
(for [x (re-split #\",\" s)] (Integer. x)))"
"(Integer. x)"
"to-i")
"(defn to-i
  [x]
  (Integer. x))

(defn add [s]
(for [x (re-split #\",\" s)] (to-i x)))")
)
(is (= (extract-method
        "(defn add [s]
(for [x (re-split #\",\" s)] (Integer. x)))"
"(re-split #\",\" s)"
"split-string")
"(defn split-string
  [s]
  (re-split #\",\" s))

(defn add [s]
(for [x (split-string s)] (Integer. x)))"
)))

(deftest no_duplicated_bindings
  (is (= (extract-method
          "(defn a [s] (if (.contains s \",\") 1 s))"
          "(if (.contains s \",\") 1 s)"
          "b")         
         "(defn b\n  [s]\n  (if (.contains s \",\") 1 s))\n\n(defn a [s] (b s))")))
