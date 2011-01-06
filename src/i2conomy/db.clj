(ns i2conomy.db
  (:use org.bituf.clj-dbcp)
  (:require [clojure.contrib.sql :as sql]))

(def file-db (db-spec (sqlite-filesystem-datasource "mint.db")))

(def memory-db (db-spec (sqlite-memory-datasource)))

(defn- table-exists? [name]
  "Checks if the specified sqlite table exists"
  (sql/with-query-results rs ["SELECT name from sqlite_master WHERE name=? and type='table'" name] 
    (not (nil? (first rs)))))

(defn- create-accounts-table []
  (sql/do-commands
    "CREATE TABLE IF NOT EXISTS accounts (username VARCHAR(50) PRIMARY KEY, [password-hash] VARCHAR(80))"))

(defn- create-balances-table []
  (sql/do-commands
    "CREATE TABLE IF NOT EXISTS balances (username VARCHAR(50), currency VARCHAR(50), amount INTEGER,
        PRIMARY KEY (username, currency))"
    "CREATE INDEX IF NOT EXISTS balances_username_idx ON balances (username)"))

(defn- create-transfers-table []
  (sql/do-commands
    "CREATE TABLE IF NOT EXISTS transfers (timestamp INTEGER, [from] VARCHAR(50), [to] VARCHAR(50),
        currency VARCHAR(50), amount INTEGER, memo VARCHAR(255))"
    "CREATE INDEX IF NOT EXISTS transfers_from_idx ON transfers ([from])"
    "CREATE INDEX IF NOT EXISTS transfers_to_idx ON transfers ([to])"))

(defn create-tables []
  (do
    (create-accounts-table)
    (create-balances-table)
    (create-transfers-table)))

(defn drop-tables! []
  (do
    (sql/do-commands
      "DROP INDEX IF EXISTS balances_username_idx"
      "DROP INDEX IF EXISTS transfers_from_idx"
      "DROP INDEX IF EXISTS transfers_to_idx"
      "DROP TABLE IF EXISTS accounts"
      "DROP TABLE IF EXISTS balances"
      "DROP TABLE IF EXISTS transfers")))

(defn get-account [username]
  (sql/with-query-results rs ["SELECT * FROM accounts WHERE username=?" username]
    (first rs)))

(defn get-balance [username currency]
  (sql/with-query-results rs ["SELECT amount FROM balances WHERE username=? AND currency=?" username currency]
    (when-let [row (first rs)]
      (:amount row))))

(defn get-balances [username]
  (sql/with-query-results rs ["SELECT currency, amount FROM balances WHERE username=? ORDER BY currency"
                              username]
    (doall rs)))

(defn create-account [username password-hash]
  (do 
    (sql/insert-rows :accounts [username password-hash])
    (sql/insert-rows :balances [username username 0])))

(defn create-transfer [timestamp from to currency amount memo]
  (sql/insert-rows :transfers [timestamp from to currency amount memo]))

(defn update-balance [username currency amount]
  (if-let [balance (get-balance username currency)]
    (sql/update-values :balances
      ["username=? AND currency=?" username currency]
      {:amount (+ balance amount)})
    (sql/insert-rows :balances [username currency amount])))

(defn get-transfers [username]
  (sql/with-query-results rs ["SELECT * FROM transfers WHERE [from]=? OR [to]=? ORDER BY timestamp DESC"
                              username username]
    (doall rs)))

