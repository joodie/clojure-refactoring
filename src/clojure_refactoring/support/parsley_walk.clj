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

(ns clojure-refactoring.support.parsley-walk
  (:import net.cgrand.parsley.Node))

(defn- walk
  "Adapted from clojure.walk/walk to avoid Empty Error:
  http://stackoverflow.com/questions/16879272/how-to-postwalk-parsely-result"

  [inner outer form]
  (cond
   (list? form) (outer (apply list (map inner form)))
   (instance? clojure.lang.IMapEntry form) (outer (vec (map inner form)))
   (seq? form) (outer (doall (map inner form)))
   (map? form) (outer (into #net.cgrand.parsley.Node{} (map inner form)))
   (coll? form) (outer (into (empty form) (map inner form)))
   :else (outer form)))

(defn- postwalk [f form]
  (walk (partial postwalk f) f form))


(defn postwalk-replace [smap form]
  (postwalk (fn [x] (if (contains? smap x) (smap x) x)) form))
