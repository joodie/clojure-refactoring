(ns clojure-refactoring.support.core
  (:use [clojure.contrib pprint]
        [clojure.contrib.seq-utils :only [find-first]]
        [clojure.walk :only [postwalk-replace]]))

(defn format-code
  "Outputs code roughly how a human would format it."
   [node]
  (with-out-str
    (with-pprint-dispatch *code-dispatch*
      (pprint node))))

;; Below stolen from arc
(defn- predicater-by [f] ;; used to build any-of and all-of
  (fn [& fns] (fn [& args]
                (f identity (map apply fns (repeat args))))))

(def ^{:doc "Takes predicates and returns a function
              that returns true if any of the predicates are true"}
  any-of?
  (predicater-by some))

(def ^{:doc "Takes predicates and returns a function
              that returns true if all of the predicates are true"}
  all-of? 
  (predicater-by every?))

(defn sub-nodes [tree]
  (tree-seq (any-of? sequential? map? set?)
            seq tree))

(defn count=
  "Checks if the count of seq is equal to n"
  [seq n]
  (= (count seq) n))

(def binding-forms
     #{'let 'fn 'binding 'for 'doseq 'dotimes 'defn 'loop 'defmacro
       'if-let 'when-let 'defn- 'defmethod 'defmethod-})

(defn evens 
  "Returns every other item of coll"
  [coll]
  (take-nth 2 coll))

(defn tree-contains?
  "True if coll contains obj at some level of nesting"
   [coll obj]
  (some #{obj} (sub-nodes coll)))

(defn replace-when
  "Replaces each element of coll if pred returns true on it."
  [pred f coll]
  (map
   (fn [elem]
     (if (pred elem) (f elem) elem)) coll))

(defn first=
  [x y]
  (= (first x) y))

(defn but-second
  [coll]
  (->> (first coll)
       (conj (drop 2 coll))))

(defn after-each 
  "After each item in coll that matches predicate
   add elems."
  [pred elems coll]
  (reduce
   (fn [accum elem]
     (if (pred elem)
       `(~@accum ~elem ~elems)
       `(~@accum ~elem)))
   ()
   coll))
