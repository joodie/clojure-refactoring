0.6 or later
---
Inline function.


Ideas:
1. Get toplevel form and invokation sexp.(elisp)

2. Find definition of the invoked function (reuse from global-rename?) and extract body.

3. Compose new invoking code with parameters in function body replaced by arguments.

4. Replace sexp and return code as string.

3*. Find definition body based on invocation argument.
