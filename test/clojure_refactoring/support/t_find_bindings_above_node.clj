(ns clojure-refactoring.support.t-find-bindings-above-node
  (:use midje.sweet
        clojure-refactoring.support.find-bindings-above-node)
  (:require [clojure-refactoring.ast :as ast]))


(defn find-bindings-above-sexp [node expr]
  (->> (find-bindings-above-node
        (ast/sexp->parsley node)
        (ast/sexp->parsley expr))
       (map (comp symbol first :content))
       set))

(facts "should return bindings above the sexp within the toplevel form"
  (fact "should return defn args and let bindinds which appear in sexp"
    (find-bindings-above-sexp '(defn myfn [a] (+ 1 a)) '(+ 1 a)) => '#{a}
    (find-bindings-above-sexp '(let [a 1] a) 'a) => '#{a}
    (find-bindings-above-sexp '(defn a [a b] (+ a b)) '(+ a b)) => '#{a b})

  (fact "should return bindings which are in the same scope as the sexp"
    (find-bindings-above-sexp '(let [a 1]
                                 (let [b 2]
                                   (+ a b))) '(+ a b)) => '#{a b}
    (find-bindings-above-sexp '(do
                                 (let [a 1]
                                   (+ a 1))
                                 (let [b 1]
                                   (+ 1 b))) '(+ 1 b)) => '#{b})

  (fact "should return bindings which are in the nest destructure form"
    (find-bindings-above-sexp '(let [{a :a :as c :or {:a 1}} {:a 1}]
                                 (+ 1 a)) '(+ 1 a)) => '#{a c}
    (find-bindings-above-sexp '(let [{:keys [a b]} {:a 1 :b 2}]
                                 (+ a b)) '(+ a b)) => '#{a b})

  (fact  "should return vars in list comprehension"
    (find-bindings-above-sexp '(defn add [s]
                                 (for [x (re-split #"," s)]
                                   (Integer. x))) '(re-split #"," s)) => '#{s x}))
