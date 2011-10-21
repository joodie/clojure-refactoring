;;; clojure-refactoring-mode.el --- Minor mode for basic clojure
;;; refactoring

;; Copyright (C) 2009, Tom Crayford
;; Author: Tom Crayford <tcrayford@googlemail.com>
;; Version: 0.1
;; Keywords: languages, lisp

;; This file is not part of GNU Emacs

;; Commentary
;; Note this mode simply does simple text substitution at the moment.

;;; License:

;; This program is free software; you can redistribute it and/or
;; modify it under the terms of the GNU General Public License
;; as published by the Free Software Foundation; either version 3
;; of the License, or (at your option) any later version.
;;
;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.
;;
;; You should have received a copy of the GNU General Public License
;; along with GNU Emacs; see the file COPYING.  If not, write to the
;; Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
;; Boston, MA 02110-1301, USA.

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
  (let ((refactoring (ido-completing-read "Refactoring: " clojure-refactoring-refactorings-list nil t)))
    (funcall (intern (concat "clojure-refactoring-" refactoring)))))

(defun get-sexp ()
  (if mark-active
      (substring-no-properties (delete-and-extract-region (mark) (point)))
    (let ((out (format "%S" (sexp-at-point))))
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
