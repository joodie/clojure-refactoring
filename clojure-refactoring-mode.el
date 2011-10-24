;;; clojure-refactoring-mode.el --- Minor mode for basic clojure
;;; refactoring

;; Author: Tom Crayford <tcrayford@googlemail.com>
;; Version: 0.1
;; Keywords: languages, lisp

;; This file is not part of GNU Emacs

;; Commentary
;; Note this mode simply does simple text substitution at the moment.

;;; License:

;; Copyright (c) 2009, 2010, Tom Crayford,
;;           (c) 2011, Joost Diepenmaat, Zeekat Softwareontwikkeling
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

;;; Code:

(require 'thingatpt)
(require 'cl)
(defvar clojure-refactoring-mode-hook '()
  "Hooks to be run when loading clojure refactoring mode")

(defun clojure-refactoring-eval-sync (string)
  (slime-eval `(swank:eval-and-grab-output ,string)))

(setq clojure-refactoring-refactorings-list
      (list "extract-fn" "thread-last" "extract-global" "thread-first" "unthread" "extract-local" "destructure-map" "rename" "global-rename"))

(defun clojure-refactoring-ido ()
  (interactive)
  (if (and (fboundp 'slime-connected-p)
           (slime-connected-p))
      (let ((refactoring (ido-completing-read "Refactoring: " clojure-refactoring-refactorings-list nil t)))
        (funcall (intern (concat "clojure-refactoring-" refactoring))))
    (error "clojure-refactoring needs a SLIME connection.")))

(defun get-sexp ()
  (if mark-active
      (substring-no-properties (delete-and-extract-region (mark) (point)))
    (let ((out (slime-sexp-at-point)))
      (forward-kill-sexp)
      out)))

(defun clojure-refactoring-call (s)
  (car (cdr (clojure-refactoring-eval-sync s))))

(defun forward-kill-sexp ()
  (interactive)
  (forward-sexp)
  (backward-kill-sexp))

;;formatting
(defun clojure-refactoring-wrap-as-string (s)
  (assert (stringp s))
  (format "%S" s))

(defun clojure-refactoring-format-clojure-call (ns name &rest args)
  (concat
   (concat (format "(require 'clojure-refactoring.%s)(clojure-refactoring.%s/%s "
                   ns ns name) (mapconcat 'identity args " ")) ")"))

(defun clojure-refactoring-format-call-with-string-args (ns name &rest args)
  (apply 'clojure-refactoring-format-clojure-call ns name (mapcar #'clojure-refactoring-wrap-as-string args)))

(defun clojure-refactoring-call-with-string-args (&rest args)
  (clojure-refactoring-call
   (apply 'clojure-refactoring-format-call-with-string-args args)))

(defun clojure-refactoring-insert-sexp (s)
  (insert (read s)))

(defun clojure-refactoring-extract-fn ()
  "Extracts a function."
  (interactive)
  (let ((fn-name (read-from-minibuffer "Function name: "))
        (defn (slime-defun-at-point))
        (body (get-sexp)))
    (save-excursion
      (beginning-of-defun)
      (forward-kill-sexp)
      (clojure-refactoring-insert-sexp
       (clojure-refactoring-call-with-string-args
        "extract-method" "extract-method"
        defn body fn-name)))))

(defun clojure-refactoring-thread-expr (str)
  (let ((body (get-sexp)))
    (save-excursion
      (clojure-refactoring-insert-sexp
       (clojure-refactoring-call-with-string-args
        "thread-expression"
        (format "thread-%s" str)
        body))
      (beginning-of-defun)
      (indent-sexp))))

(defun clojure-refactoring-thread-last ()
  (interactive)
  (clojure-refactoring-thread-expr "last"))

(defun clojure-refactoring-thread-first ()
  (interactive)
  (clojure-refactoring-thread-expr "first"))

(defun clojure-refactoring-unthread ()
  (interactive)
  (clojure-refactoring-thread-expr "unthread"))

(defun clojure-refactoring-read-symbol-at-point ()
  (read-from-minibuffer "Old name: "
                        (symbol-name (symbol-at-point))))

(defun clojure-refactoring-rename ()
  (interactive)
  (save-excursion
    (let ((old-name (clojure-refactoring-read-symbol-at-point))
          (new-name (read-from-minibuffer "New name: ")))
      (beginning-of-defun)
      (mark-sexp)
      (let ((body (buffer-substring-no-properties (mark t) (point))))
        (forward-kill-sexp)
        (clojure-refactoring-insert-sexp
         (clojure-refactoring-call-with-string-args
          "rename"
          "rename"
          body
          old-name
          new-name))))))

(defun clojure-refactoring-reload-all-user-ns ()
  (clojure-refactoring-eval-sync "(require 'clojure-refactoring.support.source)(clojure-refactoring.support.source/reload-all-user-ns)"))

(defun clojure-refactoring-global-rename ()
  (interactive)
  (let ((old-name (clojure-refactoring-read-symbol-at-point))
        (new-name (read-from-minibuffer "New name: ")))
    (save-some-buffers 't)
    (let ((expr (format "(require 'clojure-refactoring.rename) (ns clojure-refactoring.rename) (global-rename '%s '%s '%s)"
                        (slime-current-package) old-name new-name)))
      (clojure-refactoring-process-global-replacements
       (read (clojure-refactoring-call
              expr)))))
  (save-some-buffers 't)
  (clojure-refactoring-reload-all-user-ns))

(defun clojure-refactoring-extract-global ()
  (let ((var-name (read-from-minibuffer "Variable name: "))
        (body (delete-and-extract-region (mark t) (point))))
    (save-excursion
      (beginning-of-buffer)
      (forward-sexp)
      (paredit-mode 0)
      (insert "(def " var-name body ")")
      (reindent-then-newline-and-indent)
      (paredit-mode 1))
    (insert var-name)))

(defun clojure-refactoring-extract-local ()
  (let ((var-name (read-from-minibuffer "Variable name: "))
        (defn (slime-defun-at-point))
        (body (get-sexp)))
    (save-excursion
      (beginning-of-defun)
      (forward-kill-sexp)
      (clojure-refactoring-insert-sexp
       (clojure-refactoring-call-with-string-args
        "local-binding"
        "local-wrap"
        defn
        body
        var-name)))))

(defun clojure-refactoring-destructure-map ()
  (let ((var-name (read-from-minibuffer "Map name: "))
        (defn (slime-defun-at-point)))
    (save-excursion
      (beginning-of-defun)
      (forward-kill-sexp)
      (clojure-refactoring-insert-sexp
       (clojure-refactoring-call-with-string-args
        "destructuring"
        "destructure-map"
        defn)))))

(defun get-from-alist (key alist)
  (car (cdr (assoc key alist))))

(defun clojure-refactoring-process-global-replace (replace)
  (if (get-from-alist :new-source replace)
      (progn
        (if (string= (file-truename (buffer-file-name))
                     (file-truename (get-from-alist :file replace)))
            nil
          (find-file (get-from-alist :file replace)))
        (goto-char (point-min))
        (erase-buffer)
        (insert (get-from-alist :new-source replace)))))

(defun clojure-refactoring-process-global-replacements (replacements)
  (save-window-excursion
    (mapcar #'clojure-refactoring-process-global-replace replacements)))

(defvar clojure-refactoring-mode-map
  (let ((map (make-sparse-keymap)))
    (define-key map (kbd "C-c C-f") 'clojure-refactoring-ido)
    map)
  "Keymap for Clojure refactoring mode.")

;;;###autoload
(define-minor-mode clojure-refactoring-mode
  "A minor mode for a clojure refactoring tool")

(progn (defun clojure-refactoring-enable ()
         (clojure-refactoring-mode t))
       (add-hook 'clojure-mode-hook 'clojure-refactoring-enable))

(provide 'clojure-refactoring-mode)
;;; clojure-refactoring-mode.el ends here
