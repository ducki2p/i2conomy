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
    (is (= -100 (balance "alice" "alice")))
    (is (= 100 (balance "bob" "alice"))))

(deftest two-payments
  (do
    (reset-all!)
    (create-account "alice")
    (create-account "bob")
    (pay "alice" "bob" "alice" 100 "groceries")
    (pay "bob" "alice" "alice" 5 "refund")
    (is (= 2 (count @transfers))))
    (is (= -95 (balance "alice" "alice")))
    (is (= 95 (balance "bob" "alice"))))

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
                          (balance "alice" "alice")))))

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
    (is (= 0 (balance "alice" "alice")))))

(deftest simple-history
  (do
    (reset-all!)
    (create-account "alice")
    (create-account "bob")
    (create-account "charlie")
    (is (= 0 (count (history "alice"))))
    (pay "alice" "bob" "alice" 100 "groceries")
    (pay "bob" "charlie" "alice" 90 "drinks")
    (is (= 1 (count (history "alice"))))
    (is (= 2 (count (history "bob"))))))

