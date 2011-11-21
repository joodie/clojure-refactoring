;; Copyright (c) 2010 Tom Crayford,
;;           (c) 2011 Joost Diepenmaat, Zeekat Softwareontwikkeling
;;
;; Redistribution and use in source and binary forms, with or without
;; modification, are permitted provided that the following conditions
;; are met:
;;
;;     Redistributions of source code must retain the above copyright
;;     notice, this list of conditions and the following disclaimer.
;;
;;     Redistributions in binary form must reproduce the above
;;     copyright notice, this list of conditions and the following
;;     disclaimer in the documentation and/or other materials provided
;;     with the distribution.
;;
;; THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
;; "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
;; LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
;; FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
;; COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
;; INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
;; (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
;; SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
;; HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
;; STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
;; ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
;; OF THE POSSIBILITY OF SUCH DAMAGE.


(ns clojure-refactoring.support.parser
  (:use net.cgrand.parsley))

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
          :regex #"#\"([^\"\\]*|(\\.))*\""
          :string #"\"([^\"\\]*|(\\.))*\""
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
