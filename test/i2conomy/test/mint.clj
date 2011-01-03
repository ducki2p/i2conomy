(ns i2conomy.test.mint
  (:use [i2conomy.mint] :reload)
  (:use [clojure.test :only [are deftest is]]))

(defmacro are=
  "Check multiple assertions for equality"
  [& args]
  `(are [x y] (= x y) ~@args))

(deftest no-payments
  (do
    (reset-all!)
    (are=
      0 (count @accounts)
      0 (count @transfers))))

(deftest create-duplicate-account
  (do
    (reset-all!)
    (create-account "alice")
    (is (thrown-with-msg? IllegalArgumentException #"account alice already exist"
                          (create-account "alice")))))

(deftest single-payment
  (do
    (reset-all!)
    (create-account "alice")
    (create-account "bob")
    (pay "alice" "bob" "alice" 100 "groceries")
    (are=
      1     (count @transfers)
      -100  (balance "alice" "alice")
      100   (balance "bob" "alice")
      {"alice" -100} (balances "alice"))))

(deftest multiple-payments
  (do
    (reset-all!)
    (create-account "alice")
    (create-account "bob")
    (create-account "charlie")
    (pay "alice" "bob" "alice" 100 "groceries")
    (pay "bob" "alice" "alice" 5 "refund")
    (pay "charlie" "bob" "charlie" 49 "watermelons")
    (are=
      3   (count @transfers)
      -95 (balance "alice" "alice")
      95  (balance "bob" "alice")
      49  (balance "bob" "charlie")
      {"alice" 95 "charlie" 49} (balances "bob"))))

(deftest foreign-currency-payment
  (do
    (reset-all!)
    (create-account "alice")
    (create-account "bob")
    (create-account "charlie")
    (create-account "dave")
    (pay "alice" "bob" "alice" 100 "groceries")
    (pay "bob" "charlie" "alice" 100 "drinks")
    (is (thrown-with-msg? IllegalArgumentException #"insufficient balance for charlie on currency alice"
                          (pay "charlie" "dave" "alice" 101 "too many drinks")))))

(deftest balance-nonexisting-account
  (do
    (reset-all!)
    (is (thrown-with-msg? IllegalArgumentException #"account alice does not exist"
                          (balance "alice" "alice")))
    (is (thrown-with-msg? IllegalArgumentException #"account alice does not exist"
                          (balances "alice")))))

(deftest pay-from-nonexisting-account
  (do
    (reset-all!)
    (is (thrown-with-msg? IllegalArgumentException #"account alice does not exist"
                          (pay "alice" "bob" "alice" 100 "groceries")))))

(deftest pay-to-nonexisting-account
  (do
    (reset-all!)
    (create-account "alice")
    (is (thrown-with-msg? IllegalArgumentException #"account bob does not exist"
                          (pay "alice" "bob" "alice" 100 "groceries")))))

(deftest pay-nonexisting-currency
  (do
    (reset-all!)
    (create-account "alice")
    (create-account "bob")
    (is (thrown-with-msg? IllegalArgumentException #"currency charlie does not exist"
                          (pay "alice" "bob" "charlie" 100 "groceries")))))

(deftest balance-new-account
  (do
    (reset-all!)
    (create-account "alice")
    (are=
      0  (balance "alice" "alice")
      {} (balances "alice"))))

(deftest simple-history
  (do
    (reset-all!)
    (create-account "alice")
    (create-account "bob")
    (create-account "charlie")
    (is (= 0 (count (history "alice"))))
    (pay "alice" "bob" "alice" 100 "groceries")
    (pay "bob" "charlie" "alice" 90 "drinks")
    (are=
      1 (count (history "alice"))
      2 (count (history "bob")))))

