(ns clojure-refactoring.ast
  (:use [clojure.contrib.seq-utils :only [find-first]]
        [clojure.contrib.str-utils :only [str-join]])
  (:refer-clojure
   :exclude [symbol symbol? keyword? list vector newline conj])
  (:require [clojure.core :as core])
  (:use [clojure-refactoring.support.core
         :exclude [sub-nodes tree-contains?]])
  (:require [clojure-refactoring.support.parser :as parser])
  (:import net.cgrand.parsley.Node))

(defn make-node [tag content]
  (Node. tag content))

(defn symbol [sym]
  (make-node :atom (core/list (name sym))))

(def empty-map (make-node :map (core/list "{" "}")))

(def whitespace (make-node :whitespace '(" ")))

(def composite-tag?
     ^{:doc "Returns true if tag is from a composite node"}
     (complement #{:atom :regex :space :var :char :string}))

(defn replace-content [ast new-content]
  "Replaces the content of an ast with new content."
  (assoc ast
    :content
    new-content))

(declare walk)

(defn- replacement-for-composite [tag f]
  (if (composite-tag? tag)
    #(walk f %)
    f))

(defn- replacement-for-content [tag f content]
  (replace-when
   (complement string?)
   (replacement-for-composite tag f)
   content))

(defn- walk-replace-content [f ast]
  (let [{tag :tag content :content} ast]
    (replace-content
     ast
     (replacement-for-content tag f content))))

(defn walk [f ast]
  "Walks over ast, applying f to each node."
  (if (map? ast)
    (f
     (walk-replace-content f ast))
    (vec (map #(walk f %) ast))))

(defn- expand-nodes [ast]
  (if (sequential? ast)
    (seq ast)
    (:content ast)))

(defn sub-nodes [ast]
  "Returns a lazy sequence of all the sub-nodes of an ast."
  (tree-seq (any-of? sequential? composite-tag?)
            expand-nodes
            ast))

(defn tree-contains [ast obj]
  (some #{obj} (sub-nodes ast)))

(defn tree-replace [old new ast]
  (walk
   (fn [node] (if (= node old) new node))
   ast))

(defn replace-symbol-in-ast-node [old new ast]
  (tree-replace (symbol old) (symbol new) ast))

(defn ast->string [ast]
  (str-join "" (filter string? (sub-nodes ast))))

(def sexp->parsley (comp parser/parse1 format-code))

(defn- parsley-get-first-node [ast]
  (if (map? ast) ast (first ast)))

(defn tag=
  ([x] #(tag= x %)) ;;Curried
  ([x ast]
     (= (:tag ast) x)))

(defn- parsley-atom? [ast]
  (tag= :atom ast))

(defn- ast-content [ast]
  (str-join "" (:content ast)))

(def symbol?
     (all-of? map? parsley-atom?
              (comp core/symbol? read-string ast-content)))

(def keyword?
     (all-of? parsley-atom?
              #(first= (ast-content %) \:)))

(def ignored-node?
     (any-of? string? (tag= :whitespace) (tag= :comment)))

;;TODO: needs a better name
(defn relevant-content [ast]
  "Removes whitespace and comments from the content an ast."
  (remove ignored-node? (:content ast)))

(defn intersperse [coll item]
  "After every element in coll, add item."
  (interleave coll (repeat item)))

(defn add-whitespace [coll]
  (butlast (intersperse coll whitespace)))

(defn- coll-fn [tag start end elems]
  (make-node tag `(~start ~@elems ~end)))

(defn list [coll]
  (coll-fn :list "(" ")" (add-whitespace coll)))

(defn vector [coll]
  (coll-fn :vector "[" "]" (add-whitespace coll)))

(defn list-without-whitespace [& elems]
  (coll-fn :list "(" ")" elems))

(defn vector-without-whitespace [& elems]
  (coll-fn :vector "[" "]" elems))

(def newline (make-node :whitespace '("\n")))

(defn first-vector [ast]
  (find-first (tag= :vector) (:content ast)))

(defn parsley-fn-args [ast]
  (first-vector (parsley-get-first-node ast)))

(def parsley-bindings
     (comp relevant-content parsley-fn-args))

(defn conj [{content :content :as ast} & xs]
  (replace-content ast
                   `(~(first content)
                     ~@xs ~@(butlast (drop 1 content))
                     ~(last content))))

(defn strip-whitespace [ast]
  (walk
   (fn [node]
     (if (composite-tag? (:tag node))
       (replace-content node
                        (remove (tag= :whitespace) (:content node)))
       node))
   ast))

(def empty-list (list nil))

(def drop-first-and-last (comp rest butlast))

(def first-content (comp first :content))

(defn first-symbol [ast]
  (first-content (first (relevant-content ast))))

(def binding-node?
     (all-of? map?
              (tag= :list)
              (comp binding-forms core/symbol
                    #(apply str %) :content second :content)))

(defn- expand-args-with-parse1 [args]
  "Takes arguments from a function and returns a vector that
  (in a let form) rebinds them by parsing them."
  (->> args
       (mapcat (fn [arg] `(~arg (parser/parse1 ~arg))))
       vec))

(defmacro defparsed-fn [name args docstring & body]
  "Defines a function in which all of the args are rebound by parsing them using parse1."
  `(defn ~name ~args ~docstring
     (let ~(expand-args-with-parse1 args)
       ~@body)))
