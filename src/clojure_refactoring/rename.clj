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

(ns clojure-refactoring.rename
  (:use [clojure-refactoring.support replace]
        clojure-refactoring.ast.zip
        clojure-refactoring.support.find-bindings-above-node)
  (:require [clojure.zip :as zip])
  (:require [clojure-refactoring.support.parser :as parser])
  (:require [clojure-refactoring.ast :as ast]))

(defn rename-in-ast [ast old new]
  (ast/replace-symbol-in-ast-node
   old
   new
   ast))

(defn rename [node old-name new-name]
  (let [ast (parser/parse node)
        old (symbol old-name)
        new (symbol new-name)]
    (ast/ast->string
     (rename-in-ast ast old new))))

(defn rename-node [loc new-name]
  (if ((bindings-above loc) (zip/node loc))
    loc
    (zip/replace loc (ast/symbol new-name))))

(defn rename-non-shadowed [new-sym node old-name]
  (zip-walk (ast-zip node)
            #(if (= (zip/node %) (ast/symbol old-name))
               (rename-node % new-sym) %)))

(defn renaming-fn [old-var new-sym]
  "Returns a function for renaming nodes"
  (fn [node]
    (rename-non-shadowed new-sym node (.sym old-var))))

(defn global-rename [ns old-name new-name]
  "Sends a list of alists to emacs for processing as renames."
  (let [old-var (ns-resolve ns old-name)]
    (replace-callers old-var (renaming-fn old-var new-name))))
