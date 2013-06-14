(ns clojure-refactoring.support.t-source
  (:use midje.sweet
        clojure-refactoring.support.paths
        clojure-refactoring.support.source)
  (:require [clojure-refactoring.support replace t-replace]
            [clojure-refactoring.support.parser :as parser]))

(def a nil) ;; used to test vars bounded in namespaces

(def cache-with-one-entry (atom {'a (mk-cache-entry 0 (parser/parse "(+ 1 2)") 'a)}))
(def empty-cache (atom {}))

(defn test-entry-from-cache []
  (parsley-from-cache 'a))

(fact "with an in time entry, should not add a new entry and return the old entry"
  (parsley-from-cache 'a) => (:parsley ('a @cache-with-one-entry))
  (provided
    (get-ns-cache) => @cache-with-one-entry
    (in-time? anything) => true
    (new-ns-entry anything) => anything :times 0))

(fact "should add a new entry when the entry isn't in time"
  (parsley-from-cache 'a) => :new-entry
  (provided
    (get-ns-cache) => @cache-with-one-entry
    (in-time? anything) => false
    (new-ns-entry 'a) => {:parsley :new-entry} :times 1))

(fact "always adds a new entry with an empty cache"
  (parsley-from-cache 'a) => :new-entry
  (provided
    (get-ns-cache) => @empty-cache
    (new-ns-entry 'a) => {:parsley :new-entry} :times 1))

(facts "should find namespaces who refer to"
  (fact "should require all the namespaces in the user dir"
    (namespaces-who-refer-to 'b) => ['a]
    (provided
      (find-ns-in-user-dir) => ['a]
      (require-and-return 'a) => 'a :times 1
      (bound-in? 'a 'b) => true))

  (fact "should return empty if there are no namespaces that resolve the var"
    (namespaces-who-refer-to 'b) => '()
    (provided
      (find-ns-in-user-dir) => '[a]
      (require-and-return 'a) => 'a :times 1
      (bound-in? 'a 'b) => false)))

(facts "should tell if vars are bounded in namespaces"
  (let [this-ns (find-ns 'clojure-refactoring.support.t-source)]
    (fact "should return true if var is defined in the namespace"
      (bound-in? this-ns #'a) => truthy)
    (fact "should return false if same named var in another ns"
      (bound-in?
       this-ns
       (find-var
        'clojure-refactoring.support.t-replace/a)) => falsey)
    (fact "should return false if no such name exist in current ns"
      (bound-in?
       this-ns
       (find-var
        'clojure-refactoring.support.replace/line-from-var)) => falsey)
    (fact "should return false if var founded"
      (bound-in?
       this-ns
       (find-var
        'clojure-refactoring.support.t-source/boo)) => falsey)))

(fact "should return true if vars are imported by use :only in that ns"
  (bound-in? (find-ns 'clojure-refactoring.destructuring) #'clojure.contrib.seq-utils/find-first) => truthy)

;;To-fix :related to global-rename
;; (fact "should return true if vars are imported by require :as in that ns"
;;   (bound-in? (find-ns 'clojure-refactoring.destructuring) #'clojure-refactoring.support.parsley-walk/postwalk-replace) => truthy)
