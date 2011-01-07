(ns i2conomy.mint
  (:require [i2conomy.db :as db])
  (:use [i2conomy.validation :only [defvalidation]])
  (:import org.jasypt.util.password.StrongPasswordEncryptor))

(defn account-exists? [username]
  (not (nil? (db/get-account username))))

(defn valid-login? [username password]
  (when-let [account (db/get-account username)]
    (.checkPassword (StrongPasswordEncryptor.) password (:password-hash account))))

(defn currency-exists? [name]
  (account-exists? name))

(defvalidation account-available [username]
  (not (account-exists? username)) "account %s already exists" username)

(defvalidation account-exists [username]
  (account-exists? username) "account %s does not exist" username)

(defvalidation currency-exists [currency]
  (currency-exists? currency) "currency %s does not exist" currency)

; XXX actually needed?
(defn balance
  "Returns the balance of an account for given currency"
  [username currency]
  (do
    (account-exists username)
    (or (db/get-balance username currency)
        0)))

(defvalidation sufficient-balance [from currency amount]
  (or (= from currency) (<= amount (balance from currency)))
    "insufficient balance for %s on currency %s" from currency)

(defn balances
  "Returns all balances of an account"
  [username]
  (do
    (account-exists username)
    (db/get-balances username)))

(defn create-account
  "Creates a new account"
  ([username]
    (create-account username ""))
  ([username password]
    (do
      (account-available username)
      (let [password-hash (if (empty? password) ""
                            (.encryptPassword (StrongPasswordEncryptor.) password))]
        (db/create-account username password-hash)))))

(defn pay
  "Makes a payment between accounts"
  [from to currency amount memo]
  (do
    (account-exists from)
    (account-exists to)
    (currency-exists currency)
    (sufficient-balance from currency amount)
    (let [now (java.util.Date.)]
      (db/pay now from to currency amount memo))))

(defn history
  "Gets the transfer history of an account"
  [username]
  (do
    (account-exists username)
    (db/get-transfers username)))

(defn reset-all!
  "Resets all accounts and transfers, use with care!"
  []
  (db/drop-tables!))

