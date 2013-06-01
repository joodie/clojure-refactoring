(ns clojure-refactoring.ast.zip-test
  (:use clojure-refactoring.ast.zip :reload)
  (:require [clojure.zip :as zip])
  (:require [clojure-refactoring.support.parser :as parser])
  (:require [clojure-refactoring.ast :as ast])
  (:use clojure.test))

(use-fixtures :once #(time (%)))

(deftest find-node-finds-the-first-instance-of-a-node-in-a-zipper
  (is (=
       (->  (zip/xml-zip
             (parser/parse1 "(+ 1 2)"))
            (find-node (parser/parse1 "+"))
            zip/node)
       (parser/parse1 "+"))))
