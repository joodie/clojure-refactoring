(ns clojure-refactoring.support.parsley-test
  (:use clojure-refactoring.support.parsley :reload)
  (:use clojure-refactoring.test-helpers
        clojure.test
        [clojure-refactoring.support core source paths]))

(use-fixtures :once #(time (%)))

(deftest parsley_to_string
  (prop "parsley to string composed with parse is identity"
        [s random-sexp-from-core]
        (is (= (parsley-to-string (parse s))
               s)))

  (testing "parsley to string for each file in this project"
   (doseq [file (map filename-from-ns (find-ns-in-user-dir))]
     (let [slurped (memo-slurp file)]
       (is (= (parsley-to-string (parse slurped))
              slurped))))))

(defn parsley-rec-contains [obj ast]
  "Works out if a parsley-ast contains a sexp object"
  (-> (parsley-to-string ast)
      read-string
      (tree-contains? obj)))

(deftest replace_symbol_in_ast_node

  (prop "after replacing, the old symbol doesn't occur anywhere"
        [s random-sexp-from-core
         new random-symbol]
        (let [parsed (parse s)
              old (first (read-string s))]
          (is (not
               (->>
                (replace-symbol-in-ast-node old new parsed)
                (parsley-rec-contains old))))))

  (prop "replacing a symbol with itself is identity on an ast"
        [s random-sexp-from-core]
        (let [parsed (parse s)
              sym (first (read-string s))]
          (is (= (replace-symbol-in-ast-node sym sym parsed)
                 parsed))))

  (prop "replacing a symbol with another one, then replacing the new one with the original produces the same node"
        [s random-sexp-from-core
         new random-symbol]
        (let [parsed (parse s)
              old (first (read-string s))]
          (is (= (->> parsed
                      (replace-symbol-in-ast-node old new)
                      (replace-symbol-in-ast-node new old))
                 parsed))))

  (is (= (parsley-to-string
          (replace-symbol-in-ast-node 'a 'z
                                      (parse "(defn b [c] (a 1 2))")))
         "(defn b [c] (z 1 2))")))

(deftest parsley_walk
  (prop "parsley-walk with identity returns the same ast it was passe"
        [s random-sexp-from-core]
        (let [parsed (parse s)]
          (is (= (parsley-walk identity parsed)
                 parsed)))))

(deftest parsley_list
  (is (= (parsley-list [1 2 3])
'{:tag :list, :content ("(" 1 {:tag :whitespace, :content (" ")} 2 {:tag :whitespace, :content (" ")} 3 ")")})))

(deftest parsley_vector
  (is (= (parsley-vector [1 2 3])
'{:tag :vector, :content ("[" 1 {:tag :whitespace, :content (" ")} 2 {:tag :whitespace, :content (" ")} 3 "]")})))

