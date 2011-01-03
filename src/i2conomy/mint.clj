(ns i2conomy.mint
  (:import org.jasypt.util.password.StrongPasswordEncryptor))

(def accounts (ref (sorted-map)))

(def transfers (ref ()))

(defrecord Account [username password-hash balances])

(defrecord Transfer [timestamp from to currency amount memo])

(def error-messages
  {:nonexisting-account "account %s does not exist"
   :duplicate-account "account %s already exists"
   :nonexisting-currency "currency %s does not exist"
   :insufficient-balance "insufficient balance for %s on currency %s"})

(defn- throw-error [err & attr]
  (throw (IllegalArgumentException. (apply format (error-messages err) attr))))

(defn account-exists? [username]
  (contains? @accounts username))

(defn valid-login? [username password]
  (when-let [account (get @accounts username)]
    (.checkPassword (StrongPasswordEncryptor.) password (:password-hash account))))

(defn currency-exists? [name]
  (account-exists? name))

(defn balance
  "Returns the balance of an account for given currency"
  [username currency]
    (if-let [account (get @accounts username)]
      (get @(:balances account) currency 0)
      (throw-error :nonexisting-account username)))

(defn balances
  "Returns all balances of an account"
  [username]
    (if-let [account (get @accounts username)]
      @(:balances account)
      (throw-error :nonexisting-account username)))

(defn create-account
  "Creates a new account"
  ([username]
    (create-account username ""))
  ([username password]
  (dosync
    (if (not (account-exists? username))
      (let [password-hash (.encryptPassword (StrongPasswordEncryptor.) password)
            account (Account. username password-hash (ref (sorted-map username 0)))]
        (alter accounts assoc username account))
      (throw-error :duplicate-account username)))))

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
    (cond
      (not (account-exists? from))
        (throw-error :nonexisting-account from)
      (not (account-exists? to))
        (throw-error :nonexisting-account to)
      (not (currency-exists? currency))
        (throw-error :nonexisting-currency currency)
      (and (not= from currency) (> amount (balance from currency)))
        (throw-error :insufficient-balance from currency)
      :else
        (let [now (java.util.Date.)
              transfer (Transfer. now from to currency amount memo)]
          (alter transfers conj transfer)
          (update-balance from currency (- amount))
          (update-balance to currency amount)))))

(defn history
  "Gets the history of an account"
  [username]
    (if-let [account (get @accounts username)]
      (filter #(or
                 (= username (:to %))
                 (= username (:from %)))
              @transfers)
      (throw-error :nonexisting-account username)))

(defn reset-all!
  "Resets all accounts and transfers, use with care!"
  []
  (dosync
    (ref-set accounts (sorted-map))
    (ref-set transfers ())))

