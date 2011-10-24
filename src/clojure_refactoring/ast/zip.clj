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

(ns clojure-refactoring.ast.zip
  (:require [clojure.zip :as zip]))

(clojure.contrib.def/defalias ast-zip zip/xml-zip)

(defn find-node [zipped-ast node]
  "Given a zipped ast, returns the loc of the first occurrence of node
   by a depth-first walk."
  (loop [z zipped-ast]
    (cond (zip/end? z) nil
          (= (zip/node z) node) z
          :else (recur (zip/next z)))))

(defn nodes-leading-to [ast expr]
  "Returns a seq of all nodes inside node leading to expr."
  (-> (find-node (ast-zip ast) expr)
      zip/path))

(defn zip-walk [zipper f]
  "Performs a depth first walk over zipper, calling a function
   on each sub-node."
  (loop [loc zipper]
    (if (zip/end? loc)
      (zip/root loc)
      (recur (zip/next (f loc))))))
