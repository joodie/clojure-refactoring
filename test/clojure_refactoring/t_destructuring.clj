(ns clojure-refactoring.t-destructuring
  (:use midje.sweet clojure-refactoring.destructuring)
  (:require [clojure-refactoring.ast :as ast]
            [clojure-refactoring.support.parser :as parser]))


(fact "should ignore whitespace nodes"
  (empty? (remove ast/ignored-node? (parser/parse " "))) => truthy)

(fact "should return true is node is map-lookup"
  (map-lookup? (parser/parse1 "(:a a)")) => truthy
  (map-lookup? (parser/parse1 "(b :foo)")) => truthy
  (map-lookup? (parser/parse1 "(a (:a a))")) => falsey
  (map-lookup? (parser/parse1 "(:a a a)")) => falsey
  (map-lookup? (parser/parse1 "(:foo :bar)")) => falsey
  (map-lookup? (parser/parse1 "(:foo a :bar b)")) => falsey)

(fact "should turn keyword to symbols"
  (= (key->sym '{:tag :atom :content (":a")}) '{:tag :atom :content ("a")}) => truthy)


(fact "should return all the map-lookups in a node as a set of parsley asts"
  (map ast/ast->string
       (find-lookups (parser/parse1 "(defn a [b] (:a b))"))) => '("(:a b)")
  (map ast/ast->string
       (find-lookups (parser/parse1 "(defn a [b] (+ (:foo b) (:bar b)))"))) => (just '("(:bar b)" "(:foo b)") :in-any-order))

(fact "should turn map-lookups to canonical form"
  (ast/ast->string (lookup->canoninical-form
                    (parser/parse1 "(a :a)"))) => "(:a a)")


(fact "should add key and value (which should be parsley nodes) to a parsley map."
  (ast/ast->string
   (add-to-parsley-map '{:tag :map :content ("{" "}")}
                       '{:tag :atom :content ("a")}
                       '{:tag :atom :content ("b")})) => "{a b }")

(fact "should turn lookups to a map of map-symbols to lookups"
  (map ast/ast->string
       (lookups-to-binding-map (find-lookups (parser/parse1 "(defn a [b] (:a b))")))) => '("b{a :a }")
  (map ast/ast->string
       (lookups-to-binding-map (find-lookups (parser/parse1 "(defn a [b] (+ (:foo b) (:bar b)))")))) => '("b{foo :foo bar :bar }"))

(facts "should destructure all calls to maps"
  (destructure-map "(defn a [b] (:foo b))") => "(defn a [{foo :foo }] foo)"
  (destructure-map "(defn a [b] (+ (:foo b) (:bar b)))") => "(defn a [{bar :bar foo :foo }] (+ foo bar))"
  (destructure-map "(defn a [b] (+ (b :foo) (b :bar)))") => "(defn a [{bar :bar foo :foo }] (+ foo bar))")
