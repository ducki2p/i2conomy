(ns i2conomy.mint)

(def accounts (ref (sorted-map)))

(def transfers (ref ()))

(defrecord Account [name balances])

(defrecord Transfer [timestamp from to currency amount memo])

(defn- throw-nonexisting-account-exception [name]
  (throw (IllegalArgumentException.
           (str "account " name " does not exist"))))

(defn- throw-duplicate-account-exception [name]
  (throw (IllegalArgumentException.
           (str "account " name " already exists"))))

(defn- throw-nonexisting-currency-exception [name]
  (throw (IllegalArgumentException.
           (str "currency " name " does not exist"))))

(defn- throw-insufficient-balance-exception [name currency]
  (throw (IllegalArgumentException.
           (str "insufficient balance for " name " on currency " currency))))

(defn account-exists? [name]
  (contains? @accounts name))

(defn currency-exists? [name]
  (account-exists? name))

(defn balance
  "Returns the balance of an account for given currency"
  [name currency]
    (if-let [account (get @accounts name)]
      (get @(:balances account) currency 0)
      (throw-nonexisting-account-exception name)))

(defn balances
  "Returns all balances of an account"
  [name]
    (if-let [account (get @accounts name)]
      @(:balances account)
      (throw-nonexisting-account-exception name)))

(defn create-account
  "Creates a new account"
  [name]
  (dosync
    (if (not (account-exists? name))
      (let [now (java.util.Date.)
            account (Account. name (ref (sorted-map)))]
        (alter accounts assoc name account))
      (throw-duplicate-account-exception name))))

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
        (throw-nonexisting-account-exception from)
      (not (account-exists? to))
        (throw-nonexisting-account-exception to)
      (not (currency-exists? currency))
        (throw-nonexisting-currency-exception currency)
      (and (not= from currency) (> amount (balance from currency)))
        (throw-insufficient-balance-exception from currency)
      :else
        (let [now (java.util.Date.)
              transfer (Transfer. now from to currency amount memo)]
          (alter transfers conj transfer)
          (update-balance from currency (- amount))
          (update-balance to currency amount)))))

(defn history
  "Gets the history of an account"
  [name]
    (if-let [account (get @accounts name)]
      (filter #(or
                 (= name (:to %))
                 (= name (:from %)))
              @transfers)
      (throw-nonexisting-account-exception name)))

(defn reset-all!
  "Resets all accounts and transfers, use with care!"
  []
  (dosync
    (ref-set accounts (sorted-map))
    (ref-set transfers ())))

