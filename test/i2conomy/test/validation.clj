(ns i2conomy.test.validation
  (:use [i2conomy.validation] :reload)
  (:use [clojure.test :only [are deftest is]]))

(defvalidation :non-zero [x]
  (not= x 0) "value %s cant be zero" x)

(defvalidation :non-equal [x y]
  (not= x y) "values %s and %s can't be equal" x y)

(defvalidation :even [x]
  (even? x) "value must be even")

(deftest single-argument
  (is (thrown-with-msg? IllegalArgumentException #"value 0 cant be zero"
                        (validate [:non-zero 0])))
  (is (nil? (validate [:non-zero 1]))))

(deftest multiple-arguments
  (is (thrown-with-msg? IllegalArgumentException #"values 1 and 1 can't be equal"
                        (validate [:non-equal 1 1])))
  (is (nil? (validate [:non-equal 1 2]))))

(deftest multiple-tests
  (is (thrown-with-msg? IllegalArgumentException #"value 0 cant be zero"
                        (validate [:non-zero 0] [:non-equal 1 1])))
  (is (thrown-with-msg? IllegalArgumentException #"values 1 and 1 can't be equal"
                        (validate [:non-zero 1] [:non-equal 1 1])))
  (is (nil? (validate [:non-zero 1] [:non-equal 1 2]))))

(deftest no-error-attributes
  (is (thrown-with-msg? IllegalArgumentException #"value must be even"
                        (validate [:even 1]))))
