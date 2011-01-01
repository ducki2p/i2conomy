(ns i2conomy.test.mint
  (:use [i2conomy.mint] :reload)
  (:use [clojure.test]))

(deftest no-payments
  (do
    (reset-all!)
    (is (= 0 (count @transfers)))))

(deftest single-payment
  (do
    (reset-all!)
    (pay "alice" "bob" "alice" 100 "groceries")
      (is (= 1 (count @transfers)))))
