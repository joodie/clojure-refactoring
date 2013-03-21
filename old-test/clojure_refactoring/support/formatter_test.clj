(ns clojure-refactoring.support.formatter-test
  (:use clojure-refactoring.support.formatter :reload)
  (:use clojure.test)
  (:require [clojure-refactoring.ast :as ast])
  (:use clojure-refactoring.test-helpers))

(defn format-from-sexp [s]
  (->> (ast/sexp->parsley s)
       ast/strip-whitespace
       format-ast
       ast/ast->string))

(deftest strip-whitespace-test
  (is (= (ast/ast->string
          (ast/strip-whitespace (ast/sexp->parsley '(+ 1 2))))
         "(+12)")))

(deftest it-adds-whitespace-to-a-one-line-sexp
  (is (= (format-from-sexp '(+ 1 2)) "(+ 1 2)")))

(deftest it-formats-a-defn-correctly
    (is (= (format-from-sexp '(defn w [a] (inc a)))
           "(defn w [a]\n  (inc a))")))

(deftest it-formats-threading-last
  (is (= (format-from-sexp '(->> (inc 1) dec inc dec zero?))
         "(->> (inc 1)\n   dec\n   inc\n   dec\n   zero?)"))
  (is (= (format-from-sexp '(->> (:a a) (map zero?) (filter foo?)))
         "(->> (:a a)\n   (map zero?)\n   (filter foo?))"))
  (is (= (format-from-sexp '(->> (map #(Integer. %) s)
                                 (reduce +))))))

(deftest it-formats-threading-first
  (is (= (format-from-sexp '(-> (inc 1) dec inc dec zero?))
         "(-> (inc 1)\n   dec\n   inc\n   dec\n   zero?)"))
(is (= (format-from-sexp '(-> (:a a) (map zero?) (filter foo?)))
         "(-> (:a a)\n   (map zero?)\n   (filter foo?))")))

(deftest format-threaded-inside-another-form
  (is (= (format-from-sexp '(inc (-> (inc 1) dec)))
         "(inc (-> (inc 1)\n   dec))")))
