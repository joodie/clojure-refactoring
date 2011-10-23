(ns clojure-refactoring.support.parser
  (:use net.cgrand.parsley ))

(def sexp
  (parser {:space [#{:whitespace :comment :discard} :*]
           :main :expr*}          
          :expr- #{:atom :list :vector :set :string :regex :map :meta :quote :char
                   :syntax-quote :unquote :unquote-splicing :deprecated-meta
                   :deref :var :fn}
          :atom #"[a-zA-Z0-9!$%&*+\-\./:<=>?_][a-zA-Z0-9!$%&*+\-\./:<=>?_#]*"
          :comment #"(#!|;)[^\n]*"
          :whitespace [#"[ \t\n,]+"]
          :list ["(" :expr* ")"]
          :vector ["[" :expr* "]"]
          :set ["#{" :expr* "}"]
          :regex #"#\"([^\"\\]|(\\.))*\""
          :string #"\"([^\"\\]|(\\.))*\""
          :pair- [:expr :expr]
          :map ["{" :pair* "}"]
          :discard ["#_" :expr]
          :meta ["^" :pair]
          :quote ["'" :expr]          
          :char #"\\(.|newline|space|tab|backspace|formfeed|return|u([0-9a-fA-F]{4}|[0-7]{1,2}|[0-3][0-7]{2}))(?![a-zA-Z0-9!$%&*+\-\./:<=>?_#])"
          :syntax-quote ["`" :expr]
          :tilde- #"~(?!@)"
          :unquote [:tilde :expr]
          :unquote-splicing ["~@" :expr]
          :deprecated-meta ["#^" :pair]
          :deref ["@" :expr]
          :var ["#'" :expr]
          :fn ["#(" :expr* ")"]))

(def parse (comp :content sexp))

(def parse1 (comp first parse)) ;;parses one node
