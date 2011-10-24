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

(ns clojure-refactoring.support.formatter
  (:use [clojure-refactoring.support core])
  (:require [clojure-refactoring.ast :as ast]))

(defn replace-content-by [f {content :content :as ast}]
  (if (ast/composite-tag? (:tag ast))
    (ast/replace-content ast
                     `(~(first content)
                       ~@(f (ast/drop-first-and-last content))
                       ~(last content)))
    ast))

(defn add-whitespace-to-lists [ast]
  (ast/walk
   #(replace-content-by ast/add-whitespace %)
   ast))

(defn not-replaced-in-threading? [node]
  (or (some #{(ast/first-content node)} ["->>" "->"])
      (ast/tag= :whitespace node)
      (string? node)))

(defn replace-this-when-threading [ast toplevel]
  (not (or (not-replaced-in-threading? ast)
           (= ast (last (ast/relevant-content toplevel))))))

(def threading-spacing
     [ast/newline ast/whitespace ast/whitespace])

(defn format-threaded [ast]
  (let [with-whitespace
        (add-whitespace-to-lists ast)]
    (ast/replace-content
      with-whitespace
     (after-each #(replace-this-when-threading % with-whitespace)
                 threading-spacing
                 (:content with-whitespace)))))

(defmulti format-ast ast/first-symbol)

(defmethod format-ast "->" [ast]
           (format-threaded ast))

(defmethod format-ast "->>" [ast]
           (format-threaded ast))

(defmethod format-ast "defn" [ast]
           (ast/replace-content
            ast
            (after-each #(= % (ast/parsley-fn-args ast))
                        (drop-last threading-spacing)
                        (:content (add-whitespace-to-lists ast)))))

(defn format-sub-nodes [ast]
  (replace-content-by #(ast/add-whitespace (map format-ast %)) ast))

(defmethod format-ast :default [ast]
           (if (some ast/composite-tag? (ast/relevant-content ast))
             (format-sub-nodes ast)
             (add-whitespace-to-lists ast)))
