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

(ns clojure-refactoring.extract-method
  (:use [clojure-refactoring.support core formatter
         find-bindings-above-node]
        [clojure-refactoring.ast :only [defparsed-fn]]
        clojure.set
        [clojure.contrib.seq-utils :only [find-first]])
  (:require [clojure-refactoring.ast :as ast]))

(defn- find-occurences [args node]
  "Looks for any occurence of each element of args in the node"
  (seq (intersection (set args) (set (ast/sub-nodes node)))))

(defn fn-name [fn-node]
  (second (ast/relevant-content fn-node)))

(defn fn-call [fn-node]
  "Uses the arguments of a function node to return a call to it."
  (ast/list `(~(fn-name fn-node) ~@(ast/parsley-bindings fn-node))))

(defn- arg-occurences [f-node extracted-node]
  "Finds the bindings from f-node that are also in the extracted node."
  (-> (find-bindings-above-node f-node extracted-node)
      (find-occurences extracted-node)))

(defn- make-fn-node [name args body]
  "Creates an ast representing the new function"
  (format-ast
   (ast/list-without-whitespace
    (ast/symbol 'defn)
    name
    (ast/vector args)
    (ast/strip-whitespace body))))

(defn call-extracted [body toplevel extracted]
  (ast/tree-replace
   body
   (fn-call extracted)
   toplevel))

(defn- nodes-to-string [extract-node fn-node new-fun]
  "Formats the output for extract-method to print"
  (str (ast/ast->string new-fun)
       "\n\n"
       (ast/ast->string
        (call-extracted extract-node fn-node new-fun))))

(defparsed-fn extract-method [function-node extract-node new-name]
  "Extracts extract-string out of fn-string and replaces it with a
function call to the extracted method. Only works on single arity root functions"
  (let [args (arg-occurences function-node
                             extract-node)
        new-fun (make-fn-node new-name
                              args
                              extract-node)]
    (nodes-to-string extract-node function-node new-fun)))
