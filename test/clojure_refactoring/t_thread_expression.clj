(ns clojure-refactoring.t-thread-expression
  (:use midje.sweet
        clojure-refactoring.thread-expression
        clojure.contrib.str-utils
        [clojure-refactoring.support core formatter]))

(fact "should return true if it is threaded"
  (threaded? '(->> foo bar arr)) => truthy
  (threaded? '(-> foo bar arr)) => truthy
  (threaded? '(arr barr arr)) => falsey
  (threaded? :atom) => falsey)

;; Integration level tests below here
(def start-src "(reduce + (map #(Integer. %) s))")
(def end-src "(->> (map #(Integer. %) s)\n   (reduce +))")

(fact "should turn expr into thread-last form"
  (thread-last start-src) => end-src
  (thread-last "(reduce + (take 10 (filter even? (map #(* % %) (range)))))")"(->> (range)\n   (map #(* % %))\n   (filter even?)\n   (take 10)\n   (reduce +))"
  => "(->> (range)\n   (map #(* % %))\n   (filter even?)\n   (take 10)\n   (reduce +))" )

(fact "should turn expr into thread-first form"
  (thread-first "(+ (* c 1.8) 32)") => "(-> (* c 1.8)\n   (+ 32))"
  (thread-first "(first (.split (.replace (.toUpperCase \"a b c d\") \"A\" \"X\") \" \"))")
  => "(-> (.toUpperCase \"a b c d\")\n   (.replace \"A\" \"X\")\n   (.split \" \")\n   (first))")

(fact "should turn thread last form back to origianl expr"
  (thread-unthread "(->> sym (/ 1))") => "(/ 1 sym)\n"
  (thread-unthread
   "(->>
    1
    (rec-contains? (rest node))
    (for [sym *binding-forms*])
    (some #(= % true))
    (not))") =>
  "(not\n  (some\n    #(= % true)\n    (for [sym *binding-forms*] (rec-contains? (rest node) 1))))\n")

(fact "should turn thread first form back to origianl expr"
  (thread-unthread
   "(-> 1
 (/ 2))") => "(/ 1 2)\n"
  (thread-unthread "(-> 1\n(/ 2) (+ 1))") => "(+ (/ 1 2) 1)\n")
