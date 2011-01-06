(ns i2conomy.test.mint
  (:require [i2conomy.db :as db])
  (:use [i2conomy.mint] :reload)
  (:require [clojure.contrib.sql :as sql])
  (:use [clojure.test :only [are deftest is]]))

(defmacro are=
  "Check multiple assertions for equality"
  [& args]
  `(are [x y] (= x y) ~@args))

(defmacro database-test [name & body]
  `(deftest ~name
     (sql/with-connection db/memory-db
       (db/drop-tables!)
       (db/create-tables)
       ~@body)))

(database-test create-duplicate-account
  (do
    (create-account "alice")
    (is (thrown-with-msg? IllegalArgumentException #"account alice already exist"
                          (create-account "alice")))))

(database-test single-payment
  (do
    (create-account "alice")
    (create-account "bob")
    (pay "alice" "bob" "alice" 100 "groceries")
    (are=
      -100  (balance "alice" "alice")
      100   (balance "bob" "alice")
      '({:currency "alice" :amount -100}) (balances "alice"))))

(database-test multiple-payments
  (do
    (create-account "alice")
    (create-account "bob")
    (create-account "charlie")
    (pay "alice" "bob" "alice" 100 "groceries")
    (pay "bob" "alice" "alice" 5 "refund")
    (pay "charlie" "bob" "charlie" 49 "watermelons")
    (are=
      -95 (balance "alice" "alice")
      95  (balance "bob" "alice")
      49  (balance "bob" "charlie")
      '({:currency "alice" :amount 95} {:currency "bob" :amount 0}
        {:currency "charlie" :amount 49}) (balances "bob"))))

(database-test foreign-currency-payment
  (do
    (create-account "alice")
    (create-account "bob")
    (create-account "charlie")
    (create-account "dave")
    (pay "alice" "bob" "alice" 100 "groceries")
    (pay "bob" "charlie" "alice" 100 "drinks")
    (is (thrown-with-msg? IllegalArgumentException #"insufficient balance for charlie on currency alice"
                          (pay "charlie" "dave" "alice" 101 "too many drinks")))))

(database-test balance-nonexisting-account
  (do
    (is (thrown-with-msg? IllegalArgumentException #"account alice does not exist"
                          (balance "alice" "alice")))
    (is (thrown-with-msg? IllegalArgumentException #"account alice does not exist"
                          (balances "alice")))))

(database-test pay-from-nonexisting-account
  (do
    (is (thrown-with-msg? IllegalArgumentException #"account alice does not exist"
                          (pay "alice" "bob" "alice" 100 "groceries")))))

(database-test pay-to-nonexisting-account
  (do
    (create-account "alice")
    (is (thrown-with-msg? IllegalArgumentException #"account bob does not exist"
                          (pay "alice" "bob" "alice" 100 "groceries")))))

(database-test pay-nonexisting-currency
  (do
    (create-account "alice")
    (create-account "bob")
    (is (thrown-with-msg? IllegalArgumentException #"currency charlie does not exist"
                          (pay "alice" "bob" "charlie" 100 "groceries")))))

(database-test balance-new-account
  (do
    (create-account "alice")
    (are=
      0  (balance "alice" "alice")
      '({:currency "alice" :amount 0}) (balances "alice"))))

(database-test simple-history
  (do
    (create-account "alice")
    (create-account "bob")
    (create-account "charlie")
    (is (= 0 (count (history "alice"))))
    (pay "alice" "bob" "alice" 100 "groceries")
    (pay "bob" "charlie" "alice" 90 "drinks")
    (are=
      1 (count (history "alice"))
      2 (count (history "bob")))))

(database-test valid-password
  (do
    (create-account "alice" "password123")
    (is (valid-login? "alice" "password123"))))

(database-test invalid-password
  (do
    (create-account "alice" "password123")
    (is (not (valid-login? "alice" "passwordXYZ")))))
