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

(ns clojure-refactoring.local-binding
  (:use clojure.walk
        [clojure-refactoring.support core formatter]
        [clojure.contrib.seq-utils :only (find-first)]
        [clojure-refactoring.ast :only [defparsed-fn]])
  (:require [clojure-refactoring.ast :as ast]))

(defn- get-function-definition [defn-ast]
  (find-first (ast/tag= :list) (:content defn-ast)))

(defn- add-to-binding [{content :content :as binding-node}
                       value var-name]
  (ast/replace-content binding-node
    `(~(first content)
      ~@(butlast (drop 1 content))
      ~ast/whitespace
      ~var-name
      ~ast/whitespace
      ~value
      ~(last content))))

(defn is-node-the-binding-form [top-level ast]
  (= ast (ast/parsley-fn-args top-level)))

(defn- modify-existing-let-block [form value var-name]
  (ast/walk
   (fn [ast]
     (if (is-node-the-binding-form form ast)
       (add-to-binding ast value var-name)
       ast))
   form))

(defn let-wrap [form value var-name]
  (if (ast/binding-node? form)
    (modify-existing-let-block form value var-name)
    (ast/list-without-whitespace
     (ast/symbol 'let)
      ast/whitespace
      (ast/vector
       [var-name value])
      ast/whitespace
      form)))

(defn- wrap-function-body-with-let [defn-form value var-name]
  (let [fn-def (get-function-definition defn-form)]
    (ast/tree-replace
     fn-def (let-wrap fn-def value var-name)
     defn-form)))

(defparsed-fn local-wrap [top value var-name]
  "Extracts a value as a local variable inside top"
  (-> (wrap-function-body-with-let
        (ast/tree-replace
         value var-name top)
        value var-name)
      ast/ast->string))
