(ns clojure-refactoring.t-local-binding
  (:use midje.sweet
        clojure-refactoring.local-binding))

(facts "should extract a value as a local variable inside top"
  (fact "should wrap code as local"
    (local-wrap "(defn a [b] (+ b (/ b 1)))"
                "(/ b 1)"
                "c") => "(defn a [b] (let [c (/ b 1)] (+ b c)))")

  (fact "should wrap in a local let block"
    (local-wrap "(defn a [b] (let [c 1] (+ b (/ b c))))"
                "(/ b c)"
                "d") => "(defn a [b] (let [c 1 d (/ b c)] (+ b d)))"
    (local-wrap "(defn a [b] (let [c 1 z 3] (+ z c)))"
                "(+ z c)"
                "y") => "(defn a [b] (let [c 1 z 3 y (+ z c)] y))"))
