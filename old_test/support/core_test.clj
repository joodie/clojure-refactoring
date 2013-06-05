(ns clojure-refactoring.support.core-test
  (:use clojure-refactoring.support.core :reload)
  (:use clojure.test))

(defn fixture [f]
  (def test-fn-string
       "(defn valid-message? [msg]
  (partial every? #(and (:user msg) (:text msg) (:id msg))))")

  (def test-fn-node
       (read-string test-fn-string))
  (f))

(use-fixtures :once fixture #(time (%)))

(deftest sub_nodes
  (is (= (sub-nodes '(defn a [b] (+ b 1)))
         '((defn a [b] (+ b 1)) defn a [b] b (+ b 1) + b 1))))

(deftest tree_contains?
  (testing "a normal sexp"
    (is  (tree-contains? '(let [a 1] (let [b 2] (+ a b))) '(+ a b))))
  (testing "tree contains returns false if the expr is not in tree"
    (is (not (tree-contains? '(let [a 1] (if (= b a) true 0))
                             '(= b 12))))))

(deftest any-of-test
  (testing "any-of is true if any one of its predicates is true"
    (is ((any-of? map? :a nil?) {:a 1})))
  (testing "any-of is false if all of its predicates are false"
    (is (not ((any-of? nil? sequential?) {})))))

(deftest all-of-test
  (testing "all-of is true if all of its predicates are true"
   (is ((all-of? map? :a) {:a 1})))
  (testing "all-of is false if any of its predicates are false"
   (is (not ((all-of? map? :b) {:a 1})))))

(deftest but-second-test
  (testing "it returns all elements of a coll except the second."
   (is (= (but-second [1 2 3]) [1 3]))))
