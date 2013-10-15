(ns clojure-refactoring.support.t-paths
  (:use midje.sweet
        clojure-refactoring.support.paths))

(fact "should return filename"
  (extract-filename 'clojure-refactoring.support.namespaces) => "clojure_refactoring/support/namespaces.clj")


(fact "should return corresponding filename of the namespace"
  (let [path "clojure_refactoring/support/namespaces.clj"]
    (filename-from-ns
     'clojure-refactoring.support.namespaces) => path
    (provided
      (slime-find-file path) => path :time 1)))
