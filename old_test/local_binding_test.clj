(ns clojure-refactoring.local-binding-test
  (:use clojure.test)
  (:use clojure-refactoring.local-binding :reload))

(use-fixtures :once #(time (%)))

(deftest wraps_code_as_local
  (is (= (local-wrap "(defn a [b] (+ b (/ b 1)))"
                     "(/ b 1)"
                     "c")
         "(defn a [b] (let [c (/ b 1)] (+ b c)))")))

(deftest wraps_in_a_local_let_block
  (is (= (local-wrap "(defn a [b] (let [c 1] (+ b (/ b c))))"
                     "(/ b c)"
                     "d")
         "(defn a [b] (let [c 1 d (/ b c)] (+ b d)))"))
  (is (= (local-wrap "(defn a [b] (let [c 1 z 3] (+ z c)))"
                     "(+ z c)"
                     "y")
         "(defn a [b] (let [c 1 z 3 y (+ z c)] y))")))
