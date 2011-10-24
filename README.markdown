# NOTICE OF DEPRECATION

This version of clojure-refactoring is deprecated. Up to date versions
can be found at https://github.com/joodie/clojure-refactoring

# END OF NOTE


Clojure Refactoring Mode
------------------------

Provides automated refactoring for clojure code.

Available refactorings
----------------------


Extract-fn - extracts the current expression and replaces it with a
call to it.

Global rename - Replaces all calls to the symbol at point (in the
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

Emacs dependancies
---
Slime, ido, a running clojure connection, paredit, and thing at point.

Installation
---

Add clojure refactoring mode into lein's dev-dependancies then run
`lein deps`

For example (inside project.clj)
    :dev-dependencies [[swank-clojure "1.2.1"]
                       [clojure-refactoring "0.5.0"]]

Usage
---

Put clojure-refactoring-mode.el somewhere on your load path and add
    (require 'clojure-refactoring-mode)
into your .emacs.

Clojure-refactoring-ido will then be bound to C-c C-f.

Note that global rename will be slow at first, as it has to read the
source files into a cached.

NOTE: Still in alpha, has some breakages. Report any problems via
Github Issues please.


Hacking Philosophy
--------------------
- Wherever possible, have a reverse of each refactoring.
- Refactoring should be quick
- Refactoring names should correspond to known refactorings from OO
(where possible).
- Write a functional test for each refactoring (takes in a string and
outputs the correct string).

Known bugs
---
After doing a global rename, refactoring-mode doesn't reload the
namespaces in the right order, so there is sometimes a null pointer
exception after doing this. For now, you can fix this by restarting
swank.

License
---
Copyright (C) 2009-2010 Tom Crayford.

