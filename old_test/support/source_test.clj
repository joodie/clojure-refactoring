(ns clojure-refactoring.support.source-test
  (:use clojure-refactoring.support.source :reload)
  (:import clojure-refactoring.support.source.NameSpaceCacheEntry)
  (:use clojure-refactoring.test-helpers
        clojure-refactoring.support.paths
        clojure.test
        clojure.contrib.mock)
  (:require [clojure-refactoring.support replace replace-test])
  (:require [clojure-refactoring.support.parser :as parser]))

(use-fixtures :once #(time (%)))

(def a nil) ;; used to test does-ns-refer-to-var? below.

(def cache-with-one-entry (atom {'a
                                 (NameSpaceCacheEntry. 0 (parser/parse "(+ 1 2)") 'a)}))

(defn test-entry-from-cache []
  (parsley-from-cache 'a))

(deftest caching
  (binding [ns-cache cache-with-one-entry]
    (fact "with an in time entry, doesn't add a new entry"
          (test-entry-from-cache)
          (provided
           [in-time? (times 1 (returns true))
            filename-from-ns (returns "")
            new-ns-entry (times 0)])))

  (binding [ns-cache cache-with-one-entry]
    (fact "adds a new entry when the entry isn't in time"
          (test-entry-from-cache)
          (provided
           [in-time? (times 1 (returns false))
            new-ns-entry (times 1 (returns nil))])))

  (binding [ns-cache (atom {})]
    (fact "always adds a new entry with an empty cache"
          (test-entry-from-cache)
          (provided [filename-from-ns (returns "")
                     slurp (returns "")
                     new-ns-entry (times 1)]))))

(deftest namespaces_who_refer_to
  (know "it requires all the namespaces in the user dir"
        (doall (namespaces-who-refer-to 'b))
        (provided
         [find-ns-in-user-dir (returns '[a])
          require-and-return (times 1 (returns 'a))
          bound-in? (returns true)]))

  (fact "it is empty when there are no namespaces that resolve the var"
        (is (empty? (namespaces-who-refer-to 'a)))
        (provided
         [find-ns-in-user-dir (returns '[a])
          require-and-return (returns 'a)
          bound-in? (returns false)])))

(deftest does_ns_refer_to_var
  (let [this-ns (find-ns 'clojure-refactoring.support.source-test)]
    (is (bound-in? this-ns #'a))
    (testing "same named var in another ns"
      (is (not (bound-in?
                this-ns
                (find-var
                 'clojure-refactoring.support.replace-test/a)))))

    (testing "var named something that doesn't exist in the current ns"
      (is (not (bound-in?
                this-ns
                (find-var
                 'clojure-refactoring.support.replace/line-from-var)))))

    (testing "non existent var"
      (is (not (bound-in?
                this-ns
                (find-var
                 'clojure-refactoring.support.source-test/boo)))))))
