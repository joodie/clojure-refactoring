(defproject yehe/clojure-refactoring "0.9.0"
  :description "Clojure refactoring for Emacs/nREPL"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [net.cgrand/parsley "0.9.1"]]
  :profiles {:dev {:dependencies [[midje "1.5.0"]
                                  [org.clojure/tools.trace "0.7.5"]
                                  [org.clojure/tools.namespace "0.2.2"]]
                   :plugins [[lein-midje "3.0.0"]]}})
