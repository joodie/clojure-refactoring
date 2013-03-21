(defproject clojure-refactoring "1.5.0-SNAPSHOT"
  :description "Clojure refactoring for Emacs/SLIME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [net.cgrand/parsley "0.9.1"]
                 [org.clojure/tools.namespace "0.2.2"]]
  :profiles {:dev {:dependencies [[midje "1.5.0"]]
                   :plugins [[lein-midje "3.0.0"]]}})