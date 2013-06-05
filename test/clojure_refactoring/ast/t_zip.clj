(ns clojure-refactoring.ast.t-zip
  (:use midje.sweet clojure-refactoring.ast.zip)
  (:require [clojure.zip :as zip]
            [clojure-refactoring.support.parser :as parser]
            [clojure-refactoring.ast :as ast]))

(fact "should return the first instance of a node in a zipper"
  (->  (zip/xml-zip
        (parser/parse1 "(+ 1 2)"))
       (find-node (parser/parse1 "+"))
       zip/node) => (parser/parse1 "+"))
