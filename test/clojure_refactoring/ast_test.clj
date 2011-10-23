(ns clojure-refactoring.ast-test
  (:require [clojure-refactoring.ast :as ast] :reload)
  (:use clojure-refactoring.test-helpers
        clojure.test
        [clojure-refactoring.support core source paths])
  (:use [clojure-refactoring.support.parser :as parser]))

(use-fixtures :once #(time (%)))

(deftest parlsey_keyword
  (is (ast/keyword? (first (parser/parse ":a")))))

(deftest parsley_to_string
  (prop "parsley to string composed with parse is identity"
        [s random-sexp-from-core]
        (is (= (ast/ast->string (parser/parse s))
               s)))

  (testing "parsley to string for each file in this project"
   (doseq [file (map filename-from-ns (find-ns-in-user-dir))]
     (let [slurped (memo-slurp file)]
       (is (= (ast/ast->string (parser/parse slurped))
              slurped))))))

(defn parsley-rec-contains [obj ast]
  "Works out if a parsley-ast contains a sexp object"
  (-> (ast/ast->string ast)
      read-string
      (tree-contains? obj)))

(deftest replace_symbol_in_ast_node

  (prop "after replacing, the old symbol doesn't occur anywhere"
        [s random-sexp-from-core
         new random-symbol]
        (let [parsed (parser/parse s)
              old (first (read-string s))]
          (is (not
               (->>
                (ast/replace-symbol-in-ast-node old new parsed)
                (parsley-rec-contains old))))))

  (prop "replacing a symbol with itself is identity on an ast"
        [s random-sexp-from-core]
        (let [parsed (parser/parse s)
              sym (first (read-string s))]
          (is (= (ast/replace-symbol-in-ast-node sym sym parsed)
                 parsed))))

  (prop "replacing a symbol with another one, then replacing the new one with the original produces the same node"
        [s random-sexp-from-core
         new random-symbol]
        (let [parsed (parser/parse s)
              old (first (read-string s))]
          (is (= (->> parsed
                      (ast/replace-symbol-in-ast-node old new)
                      (ast/replace-symbol-in-ast-node new old))
                 parsed))))

  (is (= (ast/ast->string
          (ast/replace-symbol-in-ast-node 'a 'z
                                      (parser/parse "(defn b [c] (a 1 2))")))
         "(defn b [c] (z 1 2))")))

(deftest parsley_walk
  (prop "ast/walk with identity returns the same ast it was passe"
        [s random-sexp-from-core]
        (let [parsed (parser/parse s)]
          (is (= (ast/walk identity parsed)
                 parsed)))))

(deftest parsley_list
  (is (= (ast/list [1 2 3])
         (ast/make-node :list ["("
                               1 (ast/make-node :whitespace [" "])
                               2 (ast/make-node :whitespace [" "])
                               3 ")"]))))

(deftest parsley_vector
  (is (= (ast/vector [1 2 3])
         (ast/make-node :vector ["["
                               1 (ast/make-node :whitespace [" "])
                               2 (ast/make-node :whitespace [" "])
                               3 "]"]))))

(deftest parsley_binding_node?
  (is (ast/binding-node? (parser/parse1 "(let [a 1] a)"))))
