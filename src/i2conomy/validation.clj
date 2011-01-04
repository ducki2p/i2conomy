(ns i2conomy.validation)

; Inspired by http://stackoverflow.com/questions/1640311/

(defmacro assert* [test msg & attr]
  `(let [result# ~test]
     (when (not result#)
       (throw (IllegalArgumentException. (format ~msg ~@attr))))))

(defmulti validate* (fn [test & args] test))

(defmacro defvalidation [name args test msg & attr]
  "Defines a test for validation. To be used with validate."
  `(defmethod validate* ~name [~'_ ~@args]
     (assert* ~test ~msg ~@attr)))

(defn validate [& tests]
  "Validates all specified tests."
  (doseq [test tests] (apply validate* test)))

(comment
  (defvalidation :non-zero [x]
    (not= x 0) "value %s cant be zero" x)

  (defvalidation :non-equal [x y]
    (not= x y) "values %s and %s can't be equal" x y)

  (defn divide [x y]
    (validate [:non-zero y] [:even x] [:non-equal x y])
    (/ x y)))

