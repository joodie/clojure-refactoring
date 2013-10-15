;; Copyright (c) 2010 Tom Crayford,
;;           (c) 2011 Joost Diepenmaat
;;           (c) 2012, 2013, Ye He
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
  (:require [clojure-refactoring.ast :as ast]
            [clojure-refactoring.ast :refer [defparsed-fn]]
            [clojure-refactoring.support.find-bindings-above-node :refer [find-bindings-above-node]]
            [clojure.set :refer [intersection]]))

(defn fn-name [fn-node]
  (second (ast/relevant-content fn-node)))

(defn fn-call [fn-node]
  (ast/list `(~(fn-name fn-node) ~@(ast/parsley-bindings fn-node))))

(defn call-extracted [body toplevel extracted]
  (ast/tree-replace
   body
   (fn-call extracted)
    toplevel))

(defn occurs-free? [ast var-node]
  (let [bindings (find-bindings-above-node ast var-node)]
    (not (contains? bindings var-node))))

(defn free-vars [node]
  (filter (fn [var-node] (occurs-free? node var-node))
    (filter ast/symbol? (ast/sub-nodes node))))

(defn- binded-free-vars [args node]
  (seq (intersection (set args) (set (free-vars node)))))

(defn free-vars-binded-above [f-node extracted-node]
  (-> (find-bindings-above-node f-node extracted-node)
      (binded-free-vars extracted-node)))

(defn- make-fn-node [name args body]
  "Creates an ast representing the new function"
  (ast/list-without-whitespace
   (ast/symbol 'defn)
   ast/whitespace
   name
   (ast/make-node :whitespace ["\n  "])
   (ast/vector args)
   (ast/make-node :whitespace ["\n  "])
   body))

(defn- nodes-to-string [extracted-node fn-node new-fun]
  "Formats the output for extract-method to print"
  (str (ast/ast->string new-fun)
       "\n\n"
       (ast/ast->string
        (call-extracted extracted-node fn-node new-fun))))

(defparsed-fn extract-method [function-node extracted-node new-name]
  "Extracts extract-string out of fn-string and replaces it with a
function call to the extracted method. Only works on single arity root functions"
  (let [args (free-vars-binded-above function-node extracted-node)
        new-fun (make-fn-node new-name
                   args
                   extracted-node)]
    (nodes-to-string extracted-node function-node new-fun)))
