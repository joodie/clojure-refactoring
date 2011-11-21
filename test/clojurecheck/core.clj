;; (when (= (class clojure.test/report) clojure.lang.MultiFn)
;;   (eval
;;    '(do (require 'clojure.test)
;;         (ns clojure.test)
;;         (def old-report clojure.test/report))))

(ns clojure-refactoring.test.clojurecheck.core
  (:use [clojure.test]))

; Copyright 2010 © Meikel Brandmeyer.

; All rights reserved.
;
; Permission is hereby granted, free of charge, to any person obtaining a
; copy of this software and associated documentation files (the "Software")
; to deal in the Software without restriction, including without limitation
; the rights to use, copy, modify, merge, publish, distribute, sublicense
; and/or sell copies of the Software, and to permit persons to whom the
; Software is furnished to do so, subject to the following conditions:
;
; The above copyright notice and this permission notice shall be included
; in all copies or substantial portions of the Software.
;
; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
; OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY
; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
; THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
; FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
; DEALINGS IN THE SOFTWARE.

(ns ^{:author "Meikel Brandmeyer"
      :doc
      "clojurecheck - property based testing

  clojurecheck is an extensions to clojure.test. It provides generators
  for different values and datastructures. With their help random input
  for test cases are generated to test the behaviour of the code under
  test with more and more complex input.

  Example:
    (ns my.package
      (:use clojure.test)
      (:require [clojurecheck.core :as cc]))

    (defn angular-diff
      [a b]
      (-> (- a b) Math/abs (mod 180)))

    (deftest angular-diff-standard-test
      (are [x y] (= x y)
        (angular-diff   0   0) 0
        (angular-diff  90  90) 0
        (angular-diff   0  45) 45
        (angular-diff  45   0) 45
        (angular-diff   0 270) 90
        (angular-diff (* 360 2) (+ (* 360 4) 23)) 23))

    (deftest angular-diff-property
      (cc/property „angular-diff is smallest angel between a and b“
        [a    (cc/int)
         n    (cc/int)
         diff (cc/int :lower -180 :upper 180)]
        (let [b (+ a (* 360 n) diff)]
          (is (= (angular-diff a b) (Math/abs diff))))))

  And a result:
    my.package=> (run-tests)
    Testing my.package

    FAIL in (angular-diff-property) (core.clj:305)
    falsified 'angular-diff is smallest angel between a and b' in 5 attempts
    inputs where:
      a = -2
      n = 1
      diff = -3
    failed assertions where:
      expected: (= (angular-diff a b) (Math/abs diff))
        actual: (not (= 177 3))

    Ran 2 tests containing 7 assertions.
    1 failures, 0 errors.
    {:type :summary, :test 2, :pass 6, :fail 1, :error 0}"}
  clojurecheck.core
  (:refer-clojure
   :exclude (int float list vec set sorted-set hash-map sorted-map))
  (:use clojure.test))

(defn- gen-number
  [random lower upper size]
  (let [[low high] (if size
                     [(max (- size) lower) (min size upper)]
                     [lower upper])]
    (+ low (random (- high low)))))

(defn int
  "Generates a random integral number between lower and upper.
  The interval is limited by the size guidance."
  {:added "2.0"}
  [& {:keys [lower upper] :or {lower -32768 upper 32767}}]
  (fn [size]
    (gen-number rand-int lower upper size)))

(defn float
  "Generates a random floating point number between lower and upper.
  The interval is limited by the size guidance."
  {:added "2.0"}
  [& {:keys [lower upper] :or {lower -32768.0 upper 32767.0}}]
  (fn [size]
    (gen-number rand lower upper size)))

(def ^{:doc "Generates a random boolean value. Ignores the size guidance"
       :added "2.0"}
     bool
     (fn [size]
       (< (rand) 0.5)))

(defn frequency
  "Chooses one of the given generators based on the associated
  weights. The size guidance is passed verbatim to the chosen
  generator."
  {:added "2.0"}
  [choices]
  (let [freqs   (reductions + (vals choices))
        total   (last freqs)
        freqs   (map #(-> % (/ total) clojure.core/float) freqs)
        choices (map vector (keys choices) freqs)
        choose  (fn []
                  (let [dice (rand)]
                                        ; XXX: c cannot be nil, because it is a generator.
                    (some (fn [[c f]] (when (< dice f) c)) choices)))]
    (fn [size]
      ((choose) size))))

(defn one-of
  "Chooses one of the given generators with equal probability.
  The size guidance is passed verbatim to the chosen generator."
  {:added "2.0"}
  [choices]
  (frequency (zipmap choices (repeat 1))))

(defn element
  "Choose one of the given elements with equal probability.
  Since the elements are \"constant\" generators the size
  guidance is ignored."
  {:added "2.0"}
  [choices]
  (one-of (map constantly choices)))

(def
  ^:dynamic
  ^{:doc "Number of maximum retries to generate a valid value."
    :added "2.0"}
     retries
     2000)

(defn generate
  "Takes a spinning generator and runs it until it returns a value
  or the retry count is exceeded. A spinning generator indicates
  a value by returning a vector consisting of the keyword :value
  and the actual value. It indicates the request for a retry by
  returning a vector containing the keyword :retry."
  {:added "2.1"}
  [generator size]
  (loop [n retries]
    (if-not (zero? n)
      (let [[result value] (generator size)]
        (if (= result :retry)
          (recur (dec n))
          value))
      (throw (Exception. (str "Retries exhausted (" retries " attempts)"))))))

(defmacro let-gen
  "Takes a vector of let-like bindings. let-gen returns itself
  a generator. When called it evaluates the generators on the
  right hand side and assigns the result to the corresponding
  local. Later generator definitions may refer to previous locals
  as in a usual let.

  Similar to for and doseq you can intersperse the bindings with
  directives, which modify the behaviour.

    * :when (pred? ...):
      In case the predicate evaluates to false the generation
      process is cancelled and retried.
    * :let [...]:
      Takes a normal let-style binding and makes the bindings
      available to the following generator definitions."
  {:added "2.0"}
  [bindings & body]
  (@#'clojure.core/assert-args let-gen
                               (vector? bindings)       "a vector for its bindings"
                               (even? (count bindings)) "an even number of forms in the bindings vector")
  (let [size   (gensym "size__")
        emit-g (fn [[local gen] body]
                 `(let ~[local (clojure.core/list gen size)]
                    ~body))
        emit-p (fn [pred body]
                 `(if ~pred
                    ~body
                    [:retry]))
        emit-l (fn [bindings body]
                 `(let ~bindings
                    ~body))]
    `(let [generator# (fn [~size]
                        ~(reduce
                          (fn [body [v t :as bs]]
                            (case t
                                  :when (emit-p v body)
                                  :let  (emit-l v body)
                                  (emit-g [t v] body)))
                          `[:value (do ~@body)]
                          (partition 2 (rseq bindings))))]
       (fn [size#]
         (generate generator# size#)))))

(defn guard
  "Guard the given generator with the predicate. Each generated value is
  fed to the predicate. In case it returns false the run is retried. If
  the retry count runs out the whole test run is cancelled via an
  Exception."
  {:added "2.1"}
  [pred generator]
  (fn [size]
    (generate #(let [v (generator %)] (if (pred v) [:value v] [:retry])) size)))

(defn list
  "Generates a list based on the given generator. The length of
  the list is an integer generator. The default grows with the
  size guidance. The size guidance is passed verbatim to the
  item generator."
  {:added "2.0"}
  [item & {:keys [length] :or {length (int)}}]
  (fn [size]
    (take (length size) (repeatedly #(item size)))))

(defn vec
  "Generates a vector based on the given generator. The length of
  the vector is an integer generator. The default grows with the
  size guidance. The size guidance is passed verbatim to the item
  generator."
  {:added "2.0"}
  [item & {:keys [length] :or {length (int)}}]
  (let-gen [elems (list item :length length)]
           (clojure.core/vec elems)))

(defn set
  "Generates a set based on the given generator. The size of
  the set is an integer generator. The default grows with the
  size guidance. The size guidance is passed verbatim to the
  item generator."
  {:added "2.0"}
  [item & {:keys [length] :or {length (int)}}]
  (let-gen [elems (list item :length length)]
           (clojure.core/set elems)))

(defn sorted-set
  "Generates a sorted-set based on the given generator. The size of
  the sorted-set is an integer generator. The default grows with the
  size guidance. The size guidance is passed verbatim to the item
  generator."
  {:added "2.0"}
  [item & {:keys [length] :or {length (int)}}]
  (let-gen [elems (list item :length length)]
           (apply clojure.core/sorted-set elems)))

(defn hash-map
  "Generates a hash-map based on the given generators. The size of
  the hash-map is an integer generator. The default grows with the
  size guidance. The size guidance is passed verbatim to the key
  and value generators."
  {:added "2.0"}
  [keys vals & {:keys [length] :or {length (int)}}]
  (let-gen [len length
            ks  (list keys :length (constantly len))
            vs  (list vals :length (constantly len))]
           (zipmap ks vs)))

(defn sorted-map
  "Generates a sorted-map based on the given generators. The size of
  the sorted-map is an integer generator. The default grows with the
  size guidance. The size guidance is passed verbatim to the key and
  value generators."
  {:added "2.0"}
  [keys vals & {:keys [length] :or {length (int)}}]
  (let-gen [len length
            ks  (list keys :length (constantly len))
            vs  (list vals :length (constantly len))]
           (apply clojure.core/sorted-map (interleave ks vs))))

(defn string
  "Generates a string taking characters from the given generator. The
  length of the string is an integer generator. The default grows with
  the size guidance. The size guidance is passed verbatim to the
  character generator."
  {:added "2.1"}
  [characters & {:keys [length] :or {length (int)}}]
  (let-gen [chs (list characters :length length)]
           (apply str chs)))

(defn sized
  "Modify the size guidance according to f and pass it on to the
  given generator. If f is not a function it will be taken turned
  into a function returning the given value as constant."
  {:added "2.0"}
  [f gen]
  (let [f (if (fn? f) f (constantly f))]
    (comp gen f)))

(def
  ^:dynamic
  ^{:doc "Number of trials a property is tested with generated input.
  Default is 1000."
    :added "2.0"}
  trials
  100)

(defn size-scale
  "The scale function used to scale up the size guidance with increasing
  trials while testing a property with generated input."
  {:added "2.0"}
  [n]
  (if (even? n)
    (/ n 2)
    (/ (inc n) 2)))

(defn property*
  "The property* driver handles the work when testing a property. It
  expects:
    * a descriptive message for failure reporting
    * a list of locals (also for reporting)
    * a generator which takes the scaled size and returns the input
      for the property
    * the property test in form of a function of the generated
      input."
  {:added "2.0"}
  [msg locals gen prop]
  (let [results   (atom [])
        report-fn #(swap! results conj %)]
    (loop [n 1]
      (reset! results [])
      (if (< trials n)
        (report {:type :pass})
        (let [input (-> n size-scale gen)]
          (try
            (binding [report report-fn]
              (prop input))
            (let [failures (filter #(-> % :type (not= :pass)) @results)]
              (if (seq failures)
                (report {:type     ::property-fail
                         :message  msg
                         :locals   locals
                         :input    input
                         :attempts n
                         :failures failures})
                (recur (inc n))))
            (catch Throwable t
              (report {:type    ::property-error
                       :message msg
                       :locals  locals
                       :input   input
                       :attempt n
                       :error   t}))))))))

(defmacro property
  "Defines a property consisting of a binding vector as for let-gen
  which associates locals with the given generators. When testing the
  property the locals will be assigned the values generated.

  The body is a normal deftest body."
  {:added "2.0"}
  [msg bindings & body]
  (let [locals (remove keyword? (take-nth 2 bindings))]
    `(property* ~msg
                (quote ~locals)
                (let-gen ~bindings [~@locals])
                (fn [[~@locals]] ~@body))))

(defmethod old-report ::property-fail
  [{:keys [message locals input attempts failures] :as m}]
  (with-test-out
    (inc-report-counter :fail)
    (println "\nFAIL in" (testing-vars-str m))
    (when (seq *testing-contexts*) (println (testing-contexts-str)))
    (println "falsified" (if message (str "'" message "'") "property")
             "in" attempts "attempts")
    (println "inputs where:")
    (doseq [[local value] (map vector locals input)]
      (println " " local "=" (pr-str value)))
    (println "failed assertions where:")
    (doseq [fail failures]
      (println "  expected:" (pr-str (:expected fail)))
      (print "    actual: ")
      (let [actual (:actual fail)]
        (if (instance? Throwable actual)
          (clojure.stacktrace/print-cause-trace actual *stack-trace-depth*)
          (prn actual))))))

(defmethod old-report ::property-error
  [{:keys [message locals input attempt error] :as m}]
  (with-test-out
    (inc-report-counter :error)
    (println "\nERROR in" (testing-vars-str m))
    (when (seq *testing-contexts*) (println (testing-contexts-str)))
    (println (if message message "property") (str "(in attempt " attempt))
    (println "inputs where:")
    (doseq [[local value] (map vector locals input)]
      (println " " local "=" (pr-str value)))
    (println "error was:")
    (if (instance? Throwable error)
      (clojure.stacktrace/print-cause-trace error *stack-trace-depth*)
      (prn error))))
