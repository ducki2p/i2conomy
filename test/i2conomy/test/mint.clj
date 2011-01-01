(ns i2conomy.test.mint
  (:use [i2conomy.mint] :reload)
  (:use [clojure.test :only [deftest is]]))

(deftest no-payments
  (do
    (reset-all!)
    (is (= 0 (count @accounts)))
    (is (= 0 (count @transfers)))))

(deftest single-payment
  (do
    (reset-all!)
    (create-account "alice")
    (create-account "bob")
    (pay "alice" "bob" "alice" 100 "groceries")
      (is (= 1 (count @transfers))))
      (is (= -100 (balance "alice")))
      (is (= 100 (balance "bob"))))

(deftest balance-nonexisting
  (do
    (reset-all!)
    (is (= nil (balance "alice")))))

(deftest balance-new-account
  (do
    (reset-all!)
    (create-account "alice")
    (is (= 0 (balance "alice")))))
