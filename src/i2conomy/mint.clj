(ns i2conomy.mint)

(def accounts (ref (sorted-map)))

(def transfers (ref ()))

(defrecord Account [name balance-ref])

(defrecord Transfer [timestamp from to currency amount memo])

(defn balance
  "Gets the balance of an account"
  [name]
    (if-let [account (get @accounts name)]
      @(:balance-ref account)))

(defn create-account
  "Creates a new account"
  [name]
  (dosync
    (let [now (java.util.Date.)
          account (Account. name (ref 0))]
      (alter accounts assoc name account))))

(defn- update-balance
  "Modifies the balance of an account, to be used within a transation"
  [account amount]
  (let [balance-ref (:balance-ref account)]
    (alter balance-ref + amount)))

(defn pay
  "Makes a payment between accounts"
  [from to currency amount memo]
  (dosync
    (let [now (java.util.Date.)
          transfer (Transfer. now from to currency amount memo)
          from-account (get @accounts from)
          to-account (get @accounts to)]
      (cond
        (nil? from-account)
          (throw (IllegalArgumentException.
                  (str "account " from " does not exist")))
        (nil? to-account)
          (throw (IllegalArgumentException.
                  (str "account " to " does not exist")))
        :else (do
          (alter transfers conj transfer)
          (update-balance from-account (- amount))
          (update-balance to-account amount))))))

(defn reset-all!
  "Resets all accounts and transfers, use with care!"
  []
  (dosync
    (ref-set accounts (sorted-map))
    (ref-set transfers ())))

