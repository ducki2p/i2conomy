(ns i2conomy.validation)

; Inspired by http://stackoverflow.com/questions/1640311/

(defmacro defvalidation [name args test msg & attr]
  "Defines a test for validation."
  `(defn ~name [~@args]
    (let [result# ~test]
      (when (not result#)
        (throw (IllegalArgumentException. (format ~msg ~@attr)))))))

(comment
  (defvalidation non-zero [x]
    (not= x 0) "value %s cant be zero" x)

  (defvalidation non-equal [x y]
    (not= x y) "values %s and %s can't be equal" x y)

  (defn divide [x y]
    (do (non-zero y) (even x) (non-equal x y))
    (/ x y)))

