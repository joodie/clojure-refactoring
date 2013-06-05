(ns clojure-refactoring.support.paths-test
  (:use clojure-refactoring.support.paths :reload)
  (:use clojure-refactoring.test-helpers
        clojure.test
        clojure.contrib.mock))

(use-fixtures :once #(time (%)))

(deftest extract_filename
  (is (= (extract-filename 'clojure-refactoring.support.namespaces)
         "clojure_refactoring/support/namespaces.clj")))

(deftest filename_from_ns
  (let [path "clojure_refactoring/support/namespaces.clj"]
    (know "calls slime find file with a munged path"
          (is (= (filename-from-ns
                  'clojure-refactoring.support.namespaces)
                 path))
          (provided
           [slime-find-file
            (returns path
                     (has-args [#(= path %)] (times 1)))]))))

