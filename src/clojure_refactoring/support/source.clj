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

(ns clojure-refactoring.support.source
  (:use [clojure.contrib.find-namespaces :only [find-namespaces-in-dir]]
        [clojure-refactoring.support core paths])
  (:import java.io.File)
  (require [clojure-refactoring.support.parser :as parser]))

(defn- find-and-load [namespace]
  (when-not (find-ns namespace)
    (require namespace))
  (find-ns namespace))

(defn find-ns-in-user-dir []
  (->> (File. (System/getProperty "user.dir"))
       find-namespaces-in-dir
       (map find-and-load)))

(defn- last-modified [namespace]
  (.lastModified (File. (filename-from-ns namespace))))

(defonce ns-cache (atom {})) ;; a mapping of namespace-symbols to
;; cache entries

(defrecord NameSpaceCacheEntry [time parsley namespace])
;; Time is the time this cache entry was created, parsley is the
;; result of calling parsley after slurping the file

(defn new-ns-entry [namespace]
  (let [f (filename-from-ns namespace)
        parsed (parser/parse (slurp f))]
    (NameSpaceCacheEntry. (last-modified namespace)
                          parsed
                          namespace)))

(defn- in-time? [cached]
  (= (last-modified (:namespace cached)) (:time cached)))

(defmacro with-cached [namespace-name & body]
  `(if-let [~'cached (@ns-cache ~namespace-name)]
     ~@body))

(defn in-time? [namespace-name]
  (with-cached namespace-name
    (in-time? cached)))

(defn- entry-from-cache [namespace-name]
  (with-cached namespace-name
    (if (in-time? cached)
      cached
      (new-ns-entry namespace-name))
    (new-ns-entry namespace-name)))

(def parsley-from-cache (comp :parsley entry-from-cache))

(defn- add-to-ns-cache! [ns]
  (swap! ns-cache assoc (force-ns-name ns) (new-ns-entry ns)))

(defn- reload [ns]
  (add-to-ns-cache! ns)
  ns)

(defn require-and-return [ns]
  (if (in-time? ns) ns
      (reload (ns-name ns))))

(def reload-all-user-ns
     #(pmap require-and-return (find-ns-in-user-dir)))

(defn bound-in? [namespace v]
  (when v (= (ns-resolve namespace (.sym v)) v)))

(defn namespaces-who-refer-to [v]
  (->> (find-ns-in-user-dir)
       (pmap require-and-return)
       (filter #(bound-in? % v))))
