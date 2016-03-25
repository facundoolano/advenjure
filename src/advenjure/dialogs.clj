
(defn print-dialog
  [game-state character speech]
  (println (str character "— " speech))
  (read-line)
  game-state)

(defn eval-line
  "If line is a literal line, return the expression to print it.
  If it's a callable, return an expression that calls it."
  [line]
  (cond
    (and (seq? line) (string? (first line))) `(print-dialog ~@line)
    :else (list line)))

(defmacro if-event
  [event line]
  `(fn [game-state#]
     (if (contains? (:events game-state#) ~event)
       (-> game-state#
           ~(eval-line line)))))

(defmacro dialog
  "Expand a dialog definition into a function to execute it."
  [& lines]
  `(fn [game-state#]
     (-> game-state#
         ~@(map eval-line lines))))