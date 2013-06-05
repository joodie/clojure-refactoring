(ns clojure-refactoring.thread-expression-test
  (:use clojure-refactoring.thread-expression :reload)
  (:use [clojure-refactoring.support core formatter]
        clojure.test clojure.contrib.str-utils))

(use-fixtures :once #(time (%)))

(deftest threaded?-test
  (is (threaded? '(->> foo bar arr)))
  (is (threaded? '(-> foo bar arr)))
  (is (not (threaded? '(arr barr arr))))
  (is (not (threaded? :atom))))

;; Integration level tests below here

(def start-src "(reduce + (map #(Integer. %) s))")
(def end-src "(->> (map #(Integer. %) s)\n   (reduce +))")

(deftest thread_last
  (is (= (thread-last start-src)
         end-src)))

(deftest thread_first
  (is (= (thread-first
          "(+ (* c 1.8) 32)")
         "(-> (* c 1.8)\n   (+ 32))")))

(deftest thread_unthread_last
  (is (= (thread-unthread
           "(->> sym (/ 1))")
         "(/ 1 sym)\n"))
  (is (= (thread-unthread
          "(->>
    1
    (rec-contains? (rest node))
    (for [sym *binding-forms*])
    (some #(= % true))
    (not))")
         "(not\n  (some\n    #(= % true)\n    (for [sym *binding-forms*] (rec-contains? (rest node) 1))))\n")))

(deftest thread_unthread_first
  (is (= (thread-unthread
          "(-> 1
 (/ 2))")

         "(/ 1 2)\n"))
  (is (= (thread-unthread
          "(-> 1\n(/ 2) (+ 1))")
         "(+ (/ 1 2) 1)\n")))
