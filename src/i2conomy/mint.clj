(ns i2conomy.mint)

(def accounts (ref (sorted-map)))

(def transfers (ref ()))

(defrecord Account [name balances])

(defrecord Transfer [timestamp from to currency amount memo])

(def error-messages
  {:nonexisting-account "account %s does not exist"
   :duplicate-account "account %s already exists"
   :nonexisting-currency "currency %s does not exist"
   :insufficient-balance "insufficient balance for %s on currency %s"})

(defn- throw-error [err & attr]
  (throw (IllegalArgumentException. (apply format (error-messages err) attr))))

(defn account-exists? [name]
  (contains? @accounts name))

(defn currency-exists? [name]
  (account-exists? name))

(defn balance
  "Returns the balance of an account for given currency"
  [name currency]
    (if-let [account (get @accounts name)]
      (get @(:balances account) currency 0)
      (throw-error :nonexisting-account name)))

(defn balances
  "Returns all balances of an account"
  [name]
    (if-let [account (get @accounts name)]
      @(:balances account)
      (throw-error :nonexisting-account name)))

(defn create-account
  "Creates a new account"
  [name]
  (dosync
    (if (not (account-exists? name))
      (let [now (java.util.Date.)
            account (Account. name (ref (sorted-map)))]
        (alter accounts assoc name account))
      (throw-error :duplicate-account name))))

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
  [name]
    (if-let [account (get @accounts name)]
      (filter #(or
                 (= name (:to %))
                 (= name (:from %)))
              @transfers)
      (throw-error :nonexisting-account name)))

(defn reset-all!
  "Resets all accounts and transfers, use with care!"
  []
  (dosync
    (ref-set accounts (sorted-map))
    (ref-set transfers ())))

