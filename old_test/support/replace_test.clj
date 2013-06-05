(ns clojure-refactoring.support.replace-test
  (:use clojure-refactoring.support.replace :reload)
  (:use [clojure-refactoring.support source paths]
        clojure.test
        clojure-refactoring.test-helpers
        clojure.contrib.mock)
  (:require [clojure-refactoring.support.parser :as parser]))

(use-fixtures :once #(time (%)))

(defn replace-test-fn [top-level-node]
  (parser/parse "a"))

(deftest map_to_alist
  (testing "empty map gives an empty alist"
    (is (= (map-to-alist {}) '())))
  (testing "pairs"
    (is (= (map-to-alist {:a 1}) '((:a 1))))))

(defn replacement-map-for-tests []
  (build-replacement-map 'a replace-test-fn))

(deftest build_replacement_map
  (testing "populates the attributes correctly"
    (expect [parsley-from-cache (returns (parser/parse "(+ a 1)"))
             filename-from-ns (returns "foo")]
            (is (= (:new-source (replacement-map-for-tests)) "a"))
            (is (= (:file (replacement-map-for-tests)) "foo")))))

(deftest replace_callers
  (expect
   [namespaces-who-refer-to (returns ['a])
    map-to-alist (times 1 (returns [:replacement-alist]))
    build-replacement-map (times 1 (returns :replacement-map))]
   (is (= (replace-callers 'a replace-test-fn) [[:replacement-alist]]))))

(deftest replace_returns_nil_if_it_does_nothing
  (doseq [namespace (find-ns-in-user-dir)]
    (let [source (memo-slurp (filename-from-ns namespace))]
      (is (nil?
           (build-replacement-map namespace identity))))))


