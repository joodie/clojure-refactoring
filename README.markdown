
[![Build Status](https://travis-ci.org/luckykevin/clojure-refactoring.png)](https://travis-ci.org/luckykevin/clojure-refactoring)

Old Clojure Refactoring Mode Relived
------------------------

This forked project provides the same refactoring functionality as the original one but mostly with these improvements: new test framework Mijde, upgrade to Clojure 1.5.0, migrated from SLIME to nREPL. I know there are still many things to do to build a full-blown refactoring tool, and this repo provides only the basic functionalities. But I hope people could regard this as a very good start towards automated refactoring tool and make contributions to it. I guess it was the best repo I can find online. 

Available refactorings
----------------------

Extract-fn - extracts the current expression and replaces it with a
call to it.

Global rename(currently unavailable) - Replaces all calls to the symbol at point (in the
current project) with the new name.

Thread-last - threads the current expression via `->>`

Thread-first - threads the current expression via `->`

Unthread - unthreads the current expression.

Extract global - defines the current expression as a global var for
this namespace (using `def`).

Extract local - defines the current expression as a local variable for
the current function definition.

Destructure map - Replaces calls to keywords on a particular map with
a destructuring for that map in the args. Only works on top-level defn
forms with one type of arity. Also only works for maps called with
keywords.

Rename - Changes a name just in this sexp.

Easy Installation
---

You cat get this via [Clojar](https://clojars.org/yehe/clojure-refactoring)

The `clojure-refactoring` Emacs functions will then be available
whenever you run `M-x clojure-jack-in` to start a nREPL session.

If you don't want to install as a leiningen plugin, you can also
declare clojure-refactoring as a dependency in your project.clj,
pom.xml or use some other dependency mechanism. As long as the
clojure-refactoring jar ends up on your class path, `clojure-jack-in`
will make sure the emacs interface is loaded.

If you don't use `clojure-jack-in`, see the section "Customized
Installation" below.


Usage
---

`clojure-refactoring-prompt` will be bound to `C-c C-f` when you're
running SLIME. Press `C-c C-f` at some point in the code and you will
be prompted to select a refactoring.

Note that global rename will be slow at first, as it has to read the
source files into a cache.

NOTE: Still in alpha, has some breakages. Report any problems via
Github Issues please.

Emacs dependencies
---

clojure-mode (with nrepl 0.1.8), paredit, and thing
at point.

Having ido-mode enabled is recommended when using
clojure-refactoring-prompt.

In general, this code is tested to work on
[emacs-24 pretest on OSX] [emacs-osx] with
[`starter-kit-lisp` version 2] [starter-kit] from [marmalade] [marmalade].

[emacs-osx]: http://emacsformacosx.com/builds
[starter-kit]: https://github.com/technomancy/emacs-starter-kit
[marmalade]: http://marmalade-repo.org/

Customized Installation
---

If you don't want to use `clojure-jack-in`, you should install the jar
as usual (as a dependency in your project or as a leiningen plugin)
and then extract
`src/clojure_refactoring/payload/clojure-refactoring-mode.el` and put
it on your elisp load-path (for instance, copy to or symlink from
`$HOME/.emacs.d`) and `(require 'clojure-refactoring-mode)` in your
emacs's `init.el`.

Hacking Philosophy
--------------------

 * Wherever possible, have a reverse of each refactoring.
 * Refactoring should be quick
 * Refactoring names should correspond to known refactorings from OO
   (where possible).
 * Write a functional test for each refactoring (takes in a string and
   outputs the correct string).

Known bugs
---
Global rename is currently unavailable since I haven't found a way to integrate it with nrepl. 


License
---

    Copyright (C) 2009-2010 Tom Crayford,
              (C) 2011 Joost Diepenmaat, Zeekat Softwareontwikkeling

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

      Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

      Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
    "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
    LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
    FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
    COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
    INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
    (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
    SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
    HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
    STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
    OF THE POSSIBILITY OF SUCH DAMAGE.
