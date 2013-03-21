(ns clojure-refactoring.test.destructuring
  (:use [midje.sweet])
  (:require [clojure-refactoring.destructuring :as d]
            [clojure-refactoring.ast :as ast]
            [clojure-refactoring.support.parser :as parser]))

(future-fact (empty? (remove ast/ignored-node? (parser/parse " "))))