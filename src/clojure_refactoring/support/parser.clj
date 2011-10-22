(ns clojure-refactoring.support.parser
  (:use net.cgrand.parsley ))

(def sexp
  (parser {:space [:whitespace :discard :*]
           :main :expr*}
          :expr- #{:atom :list :vector :set :regex :map :meta :quote :char
                   :syntax-quote :unquote :unquote-splicing :deprecated-meta
                   :deref :var :fn}
          :atom #"[a-zA-Z0-9!$%&*+\-\./:<=>?_][a-zA-Z0-9!$%&*+\-\./:<=>?_#]+"
          :comment (unspaced #{"#!" ";"} #"[^\n]" :*)
          :whitespace [#"[ \t\n,]+"]
          :list ["(" :expr* ")"]
          :vector ["[" :expr* "]"]
          :set ["#{" :expr* "}"]
          :regex #"\"([^\"\\]|(\\.))+\""
          :pair- [:expr :expr]
          :map ["{" :pair* "}"]
          :discard ["#_" :expr]
          :meta ["^" :pair]
          :quote ["'" :expr]          
          :char (unspaced "\\" #{#"." "newline" "space" "tab" "backspace"
                   "formfeed" "return"
                   #"u[0-9a-fA-F]{4}"
                   #"u[0-7]{1,2}"
                   #"u[0-3][0-7]{2}"})
          :syntax-quote ["`" :expr]
          :tilde- #"~(?!@)"
          :unquote [:tilde :expr]
          :unquote-splicing ["~@" :expr]
          :deprecated-meta ["#^" :pair]
          :deref ["@" :expr]
          :var ["#'" :expr]
          :fn ["#(" :expr* ")"]))

(def parse sexp)

(def parse1 (comp first parse)) ;;parses one node
