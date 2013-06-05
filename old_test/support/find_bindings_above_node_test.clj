(ns clojure-refactoring.support.find-bindings-above-node-test
  (:use clojure-refactoring.support.find-bindings-above-node :reload)
  (:require [clojure-refactoring.ast :as ast])
  (:use clojure.test))

(use-fixtures :once #(time (%)))

(defn find-bindings-above-sexp [node expr]
  (->> (find-bindings-above-node
        (ast/sexp->parsley node)
        (ast/sexp->parsley expr))
       (map (comp symbol first :content))
       set))

(deftest find_bindings
  (is (= (find-bindings-above-sexp
          '(defn myfn [a] (+ 1 a)) '(+ 1 a))
         '#{a}))
  (is (= (find-bindings-above-sexp
          '(let [a 1] a) 'a)
         '#{a}))
  (is (= (find-bindings-above-sexp
          '(defn a [a b] (+ a b))
          '(+ a b))
         '#{a b}))
  (testing "nested binding"
    (is (= (find-bindings-above-sexp
            '(let [a 1] (let [b 2] (+ a b))) '(+ a b))
           '#{a b})))
  (is (= (find-bindings-above-sexp
          '(do (let [a 1] (+ a 1)) (let [b 1] (+ 1 b)))
          '(+ 1 b))
         '#{b}))
  (testing "destructured bindings"
    (is (= (find-bindings-above-sexp
            '(let [{a :a :as c :or {:a 1}} {:a 1}] (+ 1 a))
            '(+ 1 a))
           '#{a c}))
    (is (= (find-bindings-above-sexp
            '(let [{:keys [a b]} {:a 1 :b 2}] (+ a b))
            '(+ a b))
           '#{a b})))
  (testing "regex in find-bindings"
    (is (=
         (find-bindings-above-sexp
          '(defn add [s] (for [x (re-split #"," s)] (Integer. x)))
          '(re-split #"," s))
         '#{s x}))))

(deftest do_blocks
  (is (= (find-bindings-above-sexp
          '(do (let [a 1] (do (inc (let [b 2] (+ a b))))))
          '(+ a b))
         '#{a b})))

(deftest loop_as_bindings_with_anonymous_fns
  (is (= (find-bindings-above-sexp
          '(loop [foo 1]
             (recur #(inc foo)))
          '(recur #(inc foo)))
         '#{foo})))

(deftest anonymous-fns-with-reader-macro
  (is (= (find-bindings-above-sexp '(let [a 1] #(inc %))
                                   '#(inc %))
         '#{a})))
