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
(require 'nrepl)
(require 'paredit)

(defvar clojure-refactoring-mode-hook '()
  "Hooks to be run when loading clojure refactoring mode")

(setq clojure-refactoring-command-list
      (list "extract-fn" "thread-last" "extract-global" "thread-first" "unthread" "extract-local" "destructure-map" "rename" "global-rename"))

(setq clojure-refactoring-command-alist
      (mapcar (lambda (refactoring) (list refactoring))
              clojure-refactoring-command-list))

(defun clojure-refactoring-prompt ()
  (interactive)
  ;;To-do: test if nrepl connection exist.
  (let ((refactoring (completing-read "Refactoring: " clojure-refactoring-command-alist nil nil)))
    (when (not (string= "" refactoring))
      (call-interactively (intern (concat "clojure-refactoring-" refactoring))))))

(defun get-sexp ()
  (if mark-active
      (substring-no-properties (delete-and-extract-region (mark) (point)))
    (let ((out (sexp-at-point)))
      (forward-kill-sexp)
      out)))

;;To-do: change the following four functions to nrepl utilities.
(defun defun-at-point ()
   "Return the text of the defun at point."
   (apply #'buffer-substring-no-properties
          (region-for-defun-at-point)))

(defun region-for-defun-at-point ()
   "Return the start and end position of defun at point."
   (save-excursion
     (save-match-data
       (end-of-defun)
       (let ((end (point)))
         (beginning-of-defun)
         (list (point) end)))))

(defun sexp-at-point ()
   "Return the sexp at point as a string, otherwise nil."
   (or (nrepl-symbol-at-point)
       (let ((string (thing-at-point 'sexp)))
         (if string (substring-no-properties string) nil))))

(defun clojure-refactoring-nrepl-call (form)
  (nrepl-interactive-eval-read-print form))

(defun nrepl-interactive-eval-read-replace (form)
  "Evaluate the given FORM and process global replacement"
  (let ((buffer (current-buffer)))
    (nrepl-send-string form
                       (nrepl-interactive-eval-read-replace-handler buffer)
                       nrepl-buffer-ns)))

(defun nrepl-interactive-eval-read-replace-handler (buffer)
  "Make a handler for evaluating and process global replacement."
  (nrepl-make-response-handler buffer
                               (lambda (buffer value)
                                 (with-current-buffer buffer
                                     (let ((sexp (read value)))
                                       (if sexp
                                           (clojure-refactoring-process-global-replacements sexp)))))
                               '()
                               (lambda (buffer err)
                                 (message "%s" err))
                               '()))

(defun nrepl-interactive-eval-read-print (form)
  "Evaluate the given FORM and print sexp read from form string in current buffer."
  (let ((buffer (current-buffer)))
    (nrepl-send-string form
                       (nrepl-interactive-eval-read-print-handler buffer)
                       nrepl-buffer-ns)))

(defun nrepl-interactive-eval-read-print-handler (buffer)
  "Make a handler for evaluating and printing result in BUFFER."
  (nrepl-make-response-handler buffer
                               (lambda (buffer value)
                                 (with-current-buffer buffer
                                   (let ((sexp (read value)))
                                     (if sexp
                                         (progn
                                           (insert sexp)
                                           (beginning-of-defun)
                                           (indent-sexp))))))
                               '()
                               (lambda (buffer err)
                                 (message "%s" err))
                               '()))



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
  (clojure-refactoring-nrepl-call
   (apply 'clojure-refactoring-format-call-with-string-args args)))

(defun clojure-refactoring-extract-fn (fn-name)
  "Extracts the expression at point into a function. Moves point
to args of new function (where the doc string should be)."
  (interactive "sFunction name: ")
  (let ((defn (defun-at-point))
        (body (get-sexp)))
    (save-excursion
      (beginning-of-defun)
      (forward-kill-sexp)
      (clojure-refactoring-call-with-string-args
       "extract-method" "extract-method"
       defn body fn-name))))

(defun clojure-refactoring-thread-expr (str)
  (let ((body (get-sexp)))
    (save-excursion
      (clojure-refactoring-call-with-string-args
       "thread-expression"
       (format "thread-%s" str)
       body))))

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

(defun clojure-refactoring-rename (new-name)
  (interactive "sNew name: ")
  (save-excursion
    (let ((old-name (clojure-refactoring-read-symbol-at-point)))
      (beginning-of-defun)
      (mark-sexp)
      (let ((body (buffer-substring-no-properties (mark t) (point))))
        (forward-kill-sexp)
        (clojure-refactoring-call-with-string-args
         "rename"
         "rename"
         body
         old-name
         new-name)))))

(defun clojure-refactoring-reload-all-user-ns ()
  (nrepl-interactive-eval "(require 'clojure-refactoring.support.source)(clojure-refactoring.support.source/reload-all-user-ns)"))

(defun clojure-refactoring-global-rename (new-name)
  (interactive "sNew name: ")
  (let ((old-name (clojure-refactoring-read-symbol-at-point)))
    (save-some-buffers 't)
    (let ((expr (format "(require 'clojure-refactoring.rename) (ns clojure-refactoring.rename) (global-rename '%s '%s '%s)"
                        (nrepl-find-ns) old-name new-name)))
      (nrepl-interactive-eval-read-replace expr)))
  (clojure-refactoring-reload-all-user-ns))

(defun clojure-refactoring-extract-global (var-name)
  (interactive "sVariable name: ")
  (let ((body (delete-and-extract-region (mark t) (point))))
    (save-excursion
      (beginning-of-buffer)
      (forward-sexp)
      (paredit-mode 0)
      (insert "(def " var-name body ")")
      (reindent-then-newline-and-indent)
      (paredit-mode 1))
    (insert var-name)))

(defun clojure-refactoring-extract-local (var-name)
  (interactive "sVarable name: ")
  (let ((defn (defun-at-point))
        (body (get-sexp)))
    (save-excursion
      (beginning-of-defun)
      (forward-kill-sexp)
      (clojure-refactoring-call-with-string-args
       "local-binding"
       "local-wrap"
       defn
       body
       var-name))))

(defun clojure-refactoring-destructure-map ()
  (interactive)
  (let ((defn (defun-at-point)))
    (save-excursion
      (beginning-of-defun)
      (forward-kill-sexp)
      (clojure-refactoring-call-with-string-args
       "destructuring"
       "destructure-map"
       defn))))

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
    (define-key map (kbd "C-c C-f") 'clojure-refactoring-prompt)
    (define-key map (kbd "C-c M-m") 'clojure-refactoring-extract-fn)
    (define-key map (kbd "C-c M-v") 'clojure-refactoring-extract-local)
    (define-key map (kbd "C-c M-r") 'clojure-refactoring-rename)
    (define-key map (kbd "C-c M-g") 'clojure-refactoring-global-rename)
    (define-key map (kbd "C-c M-d") 'clojure-refactoring-destructure-map)
    (define-key map (kbd "C-c M-l") 'clojure-refactoring-thread-last)
    (define-key map (kbd "C-c M-f") 'clojure-refactoring-thread-first)
    (define-key map (kbd "C-c M-u") 'clojure-refactoring-unthread)
    map)
  "Keymap for Clojure refactoring mode.")

;;;###autoload
(define-minor-mode clojure-refactoring-mode
  "A minor mode for a clojure refactoring tool")

(progn (defun clojure-refactoring-enable ()
         (clojure-refactoring-mode t))
       (add-hook 'clojure-mode-hook 'clojure-refactoring-enable)
       (add-hook 'nrepl-mode-hook '(lambda () (when (and (string= "Clojure" mode-name)
                                                         (not (bound-and-true-p clojure-refactoring-mode)))
                                                (clojure-refactoring-enable)))))

(provide 'clojure-refactoring-mode)
;;; clojure-refactoring-mode.el ends here
