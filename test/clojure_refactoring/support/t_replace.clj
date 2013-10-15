(ns clojure-refactoring.support.t-replace
  (:use midje.sweet
        [clojure-refactoring.support source paths replace])
  (:require [clojure-refactoring.support.parser :as parser]))

;;For t_source test.
(def a nil)

(defn replacement-code [top-level-node]
  (parser/parse "a"))

(fact "map_to_alist"
  (map-to-alist {}) => '()
  (map-to-alist {:a 1}) => '((:a 1)))

(defn replacement-map-for-tests []
  (build-replacement-map 'a replacement-code))

(fact "should build a replacement map for emacs for a given namespace"
  (build-replacement-map 'a replacement-code) => {:new-source "a" :file "foo"}
  (provided
    (filename-from-ns 'a) => "foo"
    (parsley-from-cache 'a) => (parser/parse "(+ a 1)")))

(fact "should replace all callers of a var by calling a function on them."
  (replace-callers 'a replacement-code) => [[:replacement-alist]]
  (provided
    (namespaces-who-refer-to 'a) => ['a]
    (build-replacement-map 'a replacement-code) => :replacement-map
    (map-to-alist :replacement-map) => [:replacement-alist]))

(fact "should return nil if no nothing changes"
  (build-replacement-map 'a identity) => nil
  (provided
    (parsley-from-cache 'a) => (parser/parse "(+ a 1)")))
