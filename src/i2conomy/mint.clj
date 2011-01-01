(ns i2conomy.mint)

(def transfers (ref ()))

(defrecord Transfer [timestamp from to currency amount memo])

(defn pay
  "Makes a payment between accounts"
  [from to currency amount memo]
    (dosync
      (let [now (java.util.Date.)]
        (alter transfers conj (Transfer. now from to currency amount memo)))))

(defn reset-all!
  "Reset all transfers, use with care"
  []
  (dosync
    (ref-set transfers ())))
