(ns i2conomy.mint
  (:use [i2conomy.validation :only [defvalidation]])
  (:import org.jasypt.util.password.StrongPasswordEncryptor))

(def accounts (ref (sorted-map)))

(def transfers (ref ()))

(defrecord Account [username password-hash balances])

(defrecord Transfer [timestamp from to currency amount memo])

(defn account-exists? [username]
  (contains? @accounts username))

(defn valid-login? [username password]
  (when-let [account (get @accounts username)]
    (.checkPassword (StrongPasswordEncryptor.) password (:password-hash account))))

(defn currency-exists? [name]
  (account-exists? name))

(defvalidation account-available [username]
  (not (account-exists? username)) "account %s already exists" username)

(defvalidation account-exists [username]
  (account-exists? username) "account %s does not exist" username)

(defvalidation currency-exists [currency]
  (currency-exists? currency) "currency %s does not exist" currency)

(defn balance
  "Returns the balance of an account for given currency"
  [username currency]
    (account-exists username)
    (let [account (get @accounts username)]
      (get @(:balances account) currency 0)))

(defvalidation sufficient-balance [from currency amount]
  (or (= from currency) (<= amount (balance from currency)))
    "insufficient balance for %s on currency %s" from currency)

(defn balances
  "Returns all balances of an account"
  [username]
    (account-exists username)
    (let [account (get @accounts username)]
      @(:balances account)))

(defn create-account
  "Creates a new account"
  ([username]
    (create-account username ""))
  ([username password]
  (dosync
    (account-available username)
    (let [password-hash (.encryptPassword (StrongPasswordEncryptor.) password)
          account (Account. username password-hash (ref (sorted-map username 0)))]
      (alter accounts assoc username account)))))

(defn- add-or-set
  "Returns the sum of x and y. If x is nil it returns y."
  [x y]
  (if (nil? x) y
    (+ x y)))

(defn- update-balance
  "Modifies the balance of an account, to be used within a transation"
  [account-name currency amount]
  (let [account (get @accounts account-name)
        balances (:balances account)]
    (alter balances #(update-in %1 [currency] add-or-set %2) amount)))

(defn pay
  "Makes a payment between accounts"
  [from to currency amount memo]
  (dosync
    (account-exists from)
    (account-exists to)
    (currency-exists currency)
    (sufficient-balance from currency amount)
    (let [now (java.util.Date.)
          transfer (Transfer. now from to currency amount memo)]
      (alter transfers conj transfer)
      (update-balance from currency (- amount))
      (update-balance to currency amount))))

(defn history
  "Gets the history of an account"
  [username]
    (account-exists username)
    (filter #(or
                (= username (:to %))
                (= username (:from %)))
            @transfers))

(defn reset-all!
  "Resets all accounts and transfers, use with care!"
  []
  (dosync
    (ref-set accounts (sorted-map))
    (ref-set transfers ())))

