(ns clojure-refactoring.rename-test
  (:use clojure.test
        clojure.contrib.mock)
  (:require [clojure-refactoring.ast :as ast])
  (:require [clojure-refactoring.support.parser :as parser])
  (:use clojure-refactoring.rename :reload))

(use-fixtures :once #(time (%)))

(def a nil)

(deftest renames-basic-function-call
  (is (= (rename "(defn a [b] (+ b 1))" "a" "c")
         "(defn c [b] (+ b 1))"))
  (is (= (rename "(defn c [d] (+ d 2))" "c" "z")
         "(defn z [d] (+ d 2))")))

(deftest renames-recursive-function-call
  (is (= (rename "(defn f [n] (if (<= n 1) 1 (f (dec n))))" "f" "fact")
         "(defn fact [n] (if (<= n 1) 1 (fact (dec n))))")))

(defn renaming-fn-from-strings [old-var new-name code]
  (ast/ast->string
   ((renaming-fn old-var new-name)
    (parser/parse1 code))))

(deftest renaming_fn
  (testing "it replaces occurences of the var name"
    (is (=
         (renaming-fn-from-strings #'a 'z "(defn b [c] (a 1 2))")
           "(defn b [c] (z 1 2))")))
  (testing "it doesn't replace shadowed var names"
    (is (=
         (renaming-fn-from-strings #'a 'z "(defn b [a] (a 1 2))")
         "(defn b [a] (a 1 2))"))))
;;eventually, this should only
;;replace things that resolve to vars in that namespace
