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

(def tricky-sources
  
  ["\"When compiling, generates compiled bytecode for a class with the\n  given package-qualified :name (which, as all names in these\n  parameters, can be a string or symbol), and writes the .class file\n  to the *compile-path* directory.  When not compiling, does\n  nothing. The gen-class construct contains no implementation, as the\n  implementation will be dynamically sought by the generated class in\n  functions in an implementing Clojure namespace. Given a generated\n  class org.mydomain.MyClass with a method named mymethod, gen-class\n  will generate an implementation that looks for a function named by \n  (str prefix mymethod) (default prefix: \\\"-\\\") in a\n  Clojure namespace specified by :impl-ns\n  (defaults to the current namespace). All inherited methods,\n  generated methods, and init and main functions (see :methods, :init,\n  and :main below) will be found similarly prefixed. By default, the\n  static initializer for the generated class will attempt to load the\n  Clojure support code for the class as a resource from the classpath,\n  e.g. in the example case, ``org/mydomain/MyClass__init.class``. This\n  behavior can be controlled by :load-impl-ns\n\n  Note that methods with a maximum of 18 parameters are supported.\n\n  In all subsequent sections taking types, the primitive types can be\n  referred to by their Java names (int, float etc), and classes in the\n  java.lang package can be used without a package qualifier. All other\n  classes must be fully qualified.\n\n  Options should be a set of key/value pairs, all except for :name are optional:\n\n  :name aname\n\n  The package-qualified name of the class to be generated\n\n  :extends aclass\n\n  Specifies the superclass, the non-private methods of which will be\n  overridden by the class. If not provided, defaults to Object.\n\n  :implements [interface ...]\n\n  One or more interfaces, the methods of which will be implemented by the class.\n\n  :init name\n\n  If supplied, names a function that will be called with the arguments\n  to the constructor. Must return [ [superclass-constructor-args] state] \n  If not supplied, the constructor args are passed directly to\n  the superclass constructor and the state will be nil\n\n  :constructors {[param-types] [super-param-types], ...}\n\n  By default, constructors are created for the generated class which\n  match the signature(s) of the constructors for the superclass. This\n  parameter may be used to explicitly specify constructors, each entry\n  providing a mapping from a constructor signature to a superclass\n  constructor signature. When you supply this, you must supply an :init\n  specifier. \n\n  :post-init name\n\n  If supplied, names a function that will be called with the object as\n  the first argument, followed by the arguments to the constructor.\n  It will be called every time an object of this class is created,\n  immediately after all the inherited constructors have completed.\n  It's return value is ignored.\n\n  :methods [ [name [param-types] return-type], ...]\n\n  The generated class automatically defines all of the non-private\n  methods of its superclasses/interfaces. This parameter can be used\n  to specify the signatures of additional methods of the generated\n  class. Static methods can be specified with ^{:static true} in the\n  signature's metadata. Do not repeat superclass/interface signatures\n  here.\n\n  :main boolean\n\n  If supplied and true, a static public main function will be generated. It will\n  pass each string of the String[] argument as a separate argument to\n  a function called (str prefix main).\n\n  :factory name\n\n  If supplied, a (set of) public static factory function(s) will be\n  created with the given name, and the same signature(s) as the\n  constructor(s).\n  \n  :state name\n\n  If supplied, a public final instance field with the given name will be\n  created. You must supply an :init function in order to provide a\n  value for the state. Note that, though final, the state can be a ref\n  or agent, supporting the creation of Java objects with transactional\n  or asynchronous mutation semantics.\n\n  :exposes {protected-field-name {:get name :set name}, ...}\n\n  Since the implementations of the methods of the generated class\n  occur in Clojure functions, they have no access to the inherited\n  protected fields of the superclass. This parameter can be used to\n  generate public getter/setter methods exposing the protected field(s)\n  for use in the implementation.\n\n  :exposes-methods {super-method-name exposed-name, ...}\n\n  It is sometimes necessary to call the superclass' implementation of an\n  overridden method.  Those methods may be exposed and referred in \n  the new method implementation by a local name.\n\n  :prefix string\n\n  Default: \\\"-\\\" Methods called e.g. Foo will be looked up in vars called\n  prefixFoo in the implementing ns.\n\n  :impl-ns name\n\n  Default: the name of the current ns. Implementations of methods will be \n  looked up in this namespace.\n\n  :load-impl-ns boolean\n\n  Default: true. Causes the static initializer for the generated class\n  to reference the load code for the implementing namespace. Should be\n  true when implementing-ns is the default, false if you intend to\n  load the code via some other method.\"\n"

   "(defmacro gen-class \n  \"When compiling, generates compiled bytecode for a class with the\n  given package-qualified :name (which, as all names in these\n  parameters, can be a string or symbol), and writes the .class file\n  to the *compile-path* directory.  When not compiling, does\n  nothing. The gen-class construct contains no implementation, as the\n  implementation will be dynamically sought by the generated class in\n  functions in an implementing Clojure namespace. Given a generated\n  class org.mydomain.MyClass with a method named mymethod, gen-class\n  will generate an implementation that looks for a function named by \n  (str prefix mymethod) (default prefix: \\\"-\\\") in a\n  Clojure namespace specified by :impl-ns\n  (defaults to the current namespace). All inherited methods,\n  generated methods, and init and main functions (see :methods, :init,\n  and :main below) will be found similarly prefixed. By default, the\n  static initializer for the generated class will attempt to load the\n  Clojure support code for the class as a resource from the classpath,\n  e.g. in the example case, ``org/mydomain/MyClass__init.class``. This\n  behavior can be controlled by :load-impl-ns\n\n  Note that methods with a maximum of 18 parameters are supported.\n\n  In all subsequent sections taking types, the primitive types can be\n  referred to by their Java names (int, float etc), and classes in the\n  java.lang package can be used without a package qualifier. All other\n  classes must be fully qualified.\n\n  Options should be a set of key/value pairs, all except for :name are optional:\n\n  :name aname\n\n  The package-qualified name of the class to be generated\n\n  :extends aclass\n\n  Specifies the superclass, the non-private methods of which will be\n  overridden by the class. If not provided, defaults to Object.\n\n  :implements [interface ...]\n\n  One or more interfaces, the methods of which will be implemented by the class.\n\n  :init name\n\n  If supplied, names a function that will be called with the arguments\n  to the constructor. Must return [ [superclass-constructor-args] state] \n  If not supplied, the constructor args are passed directly to\n  the superclass constructor and the state will be nil\n\n  :constructors {[param-types] [super-param-types], ...}\n\n  By default, constructors are created for the generated class which\n  match the signature(s) of the constructors for the superclass. This\n  parameter may be used to explicitly specify constructors, each entry\n  providing a mapping from a constructor signature to a superclass\n  constructor signature. When you supply this, you must supply an :init\n  specifier. \n\n  :post-init name\n\n  If supplied, names a function that will be called with the object as\n  the first argument, followed by the arguments to the constructor.\n  It will be called every time an object of this class is created,\n  immediately after all the inherited constructors have completed.\n  It's return value is ignored.\n\n  :methods [ [name [param-types] return-type], ...]\n\n  The generated class automatically defines all of the non-private\n  methods of its superclasses/interfaces. This parameter can be used\n  to specify the signatures of additional methods of the generated\n  class. Static methods can be specified with ^{:static true} in the\n  signature's metadata. Do not repeat superclass/interface signatures\n  here.\n\n  :main boolean\n\n  If supplied and true, a static public main function will be generated. It will\n  pass each string of the String[] argument as a separate argument to\n  a function called (str prefix main).\n\n  :factory name\n\n  If supplied, a (set of) public static factory function(s) will be\n  created with the given name, and the same signature(s) as the\n  constructor(s).\n  \n  :state name\n\n  If supplied, a public final instance field with the given name will be\n  created. You must supply an :init function in order to provide a\n  value for the state. Note that, though final, the state can be a ref\n  or agent, supporting the creation of Java objects with transactional\n  or asynchronous mutation semantics.\n\n  :exposes {protected-field-name {:get name :set name}, ...}\n\n  Since the implementations of the methods of the generated class\n  occur in Clojure functions, they have no access to the inherited\n  protected fields of the superclass. This parameter can be used to\n  generate public getter/setter methods exposing the protected field(s)\n  for use in the implementation.\n\n  :exposes-methods {super-method-name exposed-name, ...}\n\n  It is sometimes necessary to call the superclass' implementation of an\n  overridden method.  Those methods may be exposed and referred in \n  the new method implementation by a local name.\n\n  :prefix string\n\n  Default: \\\"-\\\" Methods called e.g. Foo will be looked up in vars called\n  prefixFoo in the implementing ns.\n\n  :impl-ns name\n\n  Default: the name of the current ns. Implementations of methods will be \n  looked up in this namespace.\n\n  :load-impl-ns boolean\n\n  Default: true. Causes the static initializer for the generated class\n  to reference the load code for the implementing namespace. Should be\n  true when implementing-ns is the default, false if you intend to\n  load the code via some other method.\"\n  {:added \"1.0\"}\n  \n  [& options]\n    (when *compile-files*\n      (let [options-map (into {} (map vec (partition 2 options)))\n            [cname bytecode] (generate-class options-map)]\n        (clojure.lang.Compiler/writeClassFile cname bytecode))))"])

(deftest test-tricky-sources
  (doseq [s tricky-sources]
    (is (parser/parse s))))
