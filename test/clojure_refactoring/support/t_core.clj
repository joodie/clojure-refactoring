(ns clojure-refactoring.support.t-core
  (:use midje.sweet
        clojure-refactoring.support.core))

(def test-fn-string
  "(defn valid-message? [msg]
  (partial every? #(and (:user msg) (:text msg) (:id msg))))")

(def test-fn-node
  (read-string test-fn-string))

(fact "test sub nodes"
  (sub-nodes '(defn a [b] (+ b 1))) => '((defn a [b] (+ b 1)) defn a [b] b (+ b 1) + b 1))

(facts "test tree_contains?"
  (fact "a normal sexp"
    (tree-contains? '(let [a 1] (let [b 2] (+ a b))) '(+ a b)) => truthy)
  (fact "tree contains returns false if the expr is not in tree"
    (tree-contains? '(let [a 1] (if (= b a) true 0)) '(= b 12)) => falsey))

(facts "test any-of-test"
  (fact "any-of is true if any one of its predicates is true"
    ((any-of? map? :a nil?) {:a 1}) => truthy)
  (fact "any-of is false if all of its predicates are false"
    ((any-of? nil? sequential?) {}) => falsey))

(facts "all-of-test"
  (fact "all-of is true if all of its predicates are true"
    ((all-of? map? :a) {:a 1}) => truthy)
  (fact "all-of is false if any of its predicates are false"
    ((all-of? map? :b) {:a 1}) => falsey))

(fact "it returns all elements of a coll except the second."
  (but-second [1 2 3]) => [1 3])
