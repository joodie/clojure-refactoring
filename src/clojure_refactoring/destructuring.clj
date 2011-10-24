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

(ns clojure-refactoring.destructuring
  (:use clojure.walk
        [clojure-refactoring.support core]
        [clojure.contrib.seq-utils :only [find-first]]
        [clojure.contrib.str-utils :only [str-join]]
        [clojure-refactoring.ast :only [defparsed-fn]])
  (:require [clojure-refactoring.ast :as ast]))

(defn map-lookup? [ast]
  (let [content (ast/relevant-content ast)]
    (and (ast/tag= :list ast)
         (count= (filter ast/keyword? content) 1)
         (count= content 2))))

(defn key->sym [kw-node]
  (ast/replace-content kw-node
    (list
     (str-join ""
            (drop 1 (first (:content kw-node)))))))

(defn find-lookups [node]
  "Returns all the map lookups in a node as a set of parsley asts"
  (->> (ast/sub-nodes node)
       (filter map-lookup?)
       set))

(defn- swap-first-with-last [ast]
  (let [[first-node last-node] (ast/relevant-content ast)]
    (replace
     {first-node last-node, last-node first-node}
     (:content ast))))

(defn- parsley-swap-first-with-last [ast]
  (ast/replace-content ast
    (swap-first-with-last ast)))

(defn lookup->canoninical-form [lookup-ast]
  (let [[maybe-keyword] (ast/relevant-content lookup-ast)]
    (if (ast/keyword? maybe-keyword)
      lookup-ast
      (parsley-swap-first-with-last lookup-ast))))

(defn add-to-parsley-map [m key val]
  "Adds key and value (which should be parsley nodes
  to m, which represents a parsley map."
  (ast/replace-content m
    `("{"
      ~key
      ~ast/whitespace
      ~val
      ~ast/whitespace
      ~@(drop 1 (:content m)))))

(def relevant-content-from-canoninical-form
     (comp ast/relevant-content lookup->canoninical-form))

(defn- add-lookup-to-binding-map [binding-map lookup]
  "Adds a lookup (a node of the form (:a a)) to a binding map."
  (let [[key m] (relevant-content-from-canoninical-form lookup)]
    (assoc binding-map m
           (add-to-parsley-map
            (get binding-map m ast/empty-map)
            (key->sym key) key))))

(defn lookups-to-binding-map [lookups]
  "Turns a set of lookups to a map of map-symbols to lookups"
  (reduce
   add-lookup-to-binding-map
   {}
   lookups))

(defn- destructured-binding-vec [old-vec lookups]
  "Replaces each key in the binding map found in old-vec with the value\nfrom the binding map"
  (postwalk-replace (lookups-to-binding-map lookups) old-vec))

(defn replace-lookups-with-destructured-symbols [lookups ast]
  ;;TODO: this bothers me, because we use this pattern of reduce
  ;;replacing stuff all over the place
  (reduce
   (fn [new-ast lookup]
     (ast/tree-replace
      lookup
      (key->sym (first (ast/relevant-content (lookup->canoninical-form lookup))))
      new-ast))
   ast
   lookups))

(defn- add-destructured-maps-to-args [lookups root-ast]
  (let [args (ast/parsley-fn-args root-ast)
        new-args (destructured-binding-vec args lookups)]
    (ast/tree-replace args new-args root-ast)))

(defparsed-fn destructure-map [root-ast]
  "Destructures all calls to maps"
  (let [lookups (find-lookups root-ast)]
    (ast/ast->string
     (replace-lookups-with-destructured-symbols
       lookups
       (add-destructured-maps-to-args lookups root-ast)))))
