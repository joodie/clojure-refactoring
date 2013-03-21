(ns clojure-refactoring.test-helpers
  (:use clojure.test
        clojure-refactoring.mock)
  (:require [clojurecheck.core :as cc])
  (:import (java.io File LineNumberReader InputStreamReader PushbackReader)
           (java.lang.reflect Modifier Method Constructor)
           (clojure.lang RT Compiler Compiler$C)))

;; from clojure.contrib.repl-utils
(defn get-source
  "Returns a string of the source code for the given symbol, if it can
  find it.  This requires that the symbol resolve to a Var defined in
  a namespace for which the .clj is in the classpath.  Returns nil if
  it can't find the source.  For most REPL usage, 'source' is more
  convenient.
   
  Example: (get-source 'filter)"
  {:deprecated "1.2"}
  [x]
  (when-let [v (resolve x)]
    (when-let [filepath (:file (meta v))]
      (when-let [strm (.getResourceAsStream (RT/baseLoader) filepath)]
        (with-open [rdr (LineNumberReader. (InputStreamReader. strm))]
          (dotimes [_ (dec (:line (meta v)))] (.readLine rdr))
          (let [text (StringBuilder.)
                pbr (proxy [PushbackReader] [rdr]
                      (read [] (let [i (proxy-super read)]
                                 (.append text (char i))
                                 i)))]
            (read (PushbackReader. pbr))
            (str text)))))))

(defonce memoized-get-source
     (memoize get-source))

(def symbol-chars (vec "abcdefghijklmnopqrstuvwxyz"))

(defn random-symbol-char [& args]
  (rand-nth symbol-chars))

(defn random-symbol [& args]
  (symbol (reduce str (take (inc (rand 10))
                            (repeatedly random-symbol-char)))))

(defn random-sexp-from-core [& args]
  (let [result (memoized-get-source
                (rand-nth
                 (keys (ns-publics 'clojure.core))))]
    (if result result
        (random-sexp-from-core))))

(defn proxy-file [time]
  (proxy [java.io.File] ["~/"] (lastModified [] time)
         (getCanonicalPath [] "absolute-path")))

(defmacro prop
  [& args]
  `(cc/property ~@args))

(defmacro modified? [reference & exprs]
  "Checks if a reference is modified whilst running exprs.
   Use can be made readable by doing
   (modified reference :during expr)"
  `(let [intial# @~reference]
     (do ~@exprs)
     (not= @~reference intial#)))

(def memo-slurp (memoize slurp))

(defmacro fact [desc test provided]
  `(expect ~(second provided)
           ~test))

