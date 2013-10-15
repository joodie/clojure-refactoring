(ns clojure-refactoring.t-rename
  (:use midje.sweet
        clojure-refactoring.rename)
  (:require [clojure-refactoring.ast :as ast]
            [clojure-refactoring.support.parser :as parser]))

(def a nil)

(defn renaming-fn-from-strings [old-var new-name code]
  (ast/ast->string
   ((renaming-fn old-var new-name)
    (parser/parse1 code))))


(facts "should rename vars in appropriate locations"
  (fact "should rename function name"
    (rename "(defn a [b] (+ b 1))" "a" "c") => "(defn c [b] (+ b 1))"
    (rename "(defn c [d] (+ d 2))" "c" "z") => "(defn z [d] (+ d 2))")

  (fact "should rename arguments"
    (rename "(defn c [d] (+ d 2))" "d" "z") => "(defn c [z] (+ z 2))")

  (fact "should rename bindings in let form"
    (rename "(defn a [x] (let [b 1] (+ b x)))" "b" "c") => "(defn a [x] (let [c 1] (+ c x)))")

  (fact "should rename recursive function call"
    (rename "(defn f [n] (if (<= n 1) 1 (f (dec n))))" "f" "fact") => "(defn fact [n] (if (<= n 1) 1 (fact (dec n))))"))


(facts "should return a function for renaming nodes"
  (fact "should replaces occurences of the var name"
    (renaming-fn-from-strings #'a 'z "(defn b [c] (a 1 2))") => "(defn b [c] (z 1 2))")
  (fact "should not replace shadowed var names"
    (renaming-fn-from-strings #'a 'z "(defn b [a] (a 1 2))") => "(defn b [a] (a 1 2))"))

;;eventually, this should only
;;replace things that resolve to vars in that namespace
