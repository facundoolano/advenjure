
(defn print-dialog
  [character speech]
  (println (str character "— " speech))
  (read-line)
  nil)

(defn eval-line
  "If line is a literal line, return the expression to print it.
  If it's a callable, return an expression that calls it."
  [line]
  (cond
    (and (seq? line) (string? (first line))) `(print-dialog ~@line)
    :else (list line)))

(defmacro dialog
  "Expand a dialog definition into a function to execute it."
  [& lines]
  `(fn []
     ~@(map eval-line lines)))