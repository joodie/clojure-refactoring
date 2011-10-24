;; Copyright (c) 2010 Tom Crayford,
;;
;; Redistribution and use in source and binary forms, with or without
;; modification, are permitted provided that the following conditions
;; are met:
;;
;;     Redistributions of source code must retain the above copyright
;;     notice, this list of conditions and the following disclaimer.
;;
;;     Redistributions in binary form must reproduce the above
;;     copyright notice, this list of conditions and the following
;;     disclaimer in the documentation and/or other materials provided
;;     with the distribution.
;;
;; THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
;; "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
;; LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
;; FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
;; COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
;; INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
;; (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
;; SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
;; HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
;; STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
;; ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
;; OF THE POSSIBILITY OF SUCH DAMAGE.

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
