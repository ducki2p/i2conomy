(ns i2conomy.wallet
  (:require [i2conomy.mint :as mint])
  (:use i2conomy.middleware
        i2conomy.session
        compojure.core
        hiccup.core
        hiccup.page-helpers
        ring.middleware.file
        ring.middleware.file-info
        ring.middleware.reload
        ring.middleware.stacktrace
        ring.middleware.session.memory
        ring.util.response
        sandbar.stateful-session))

(defn view-layout [& content]
  (html
    (doctype :xhtml-strict)
    (xhtml-tag "en"
      [:head
        [:meta {:http-equiv "Content-type"
                :content "text/html; charset=utf-8"}]
        [:title "i2conomy"]
        [:link {:href "/i2conomy.css" :rel "stylesheet" :type "text/css"}]]
      [:body
        [:p "Account: " (session-get :account "Unknown")]
        (if-let [flash (flash-get :message)]
          [:p "Message: " flash])
        content])))

(defn view-balance-input []
  (html
    [:h2 "check balance"]
    [:form {:method "post" :action "/balance"}
      [:label "Account: "] [:input {:type "text" :name "account" :value "duck"}]
      [:label "Currency: "] [:input {:type "text" :name "currency" :value "duck"}]
      [:input {:type "submit" :value "check"}]]))

(defn view-create-account-input []
  (html
    [:h2 "create account"]
    [:form {:method "post" :action "/create-account"}
      [:label "Account: "] [:input {:type "text" :name "account" :value "duck"}]
      [:input {:type "submit" :value "create"}]]))

(defn view-payment-input []
  (html
    [:h2 "make payment"]
    [:form {:method "post" :action "/pay"}
      [:label "From: "] [:input {:type "text" :name "from"}]
      [:label "To: "] [:input {:type "text" :name "to"}] [:br]
      [:label "Currency: "] [:input {:type "text" :name "currency"}]
      [:label "Amount: "] [:input {:type "text" :name "amount"}] [:br]
      [:label "Memo: "] [:input {:type "text" :name "memo"}]
      [:input {:type "submit" :value "pay"}]]))

(defroutes handler
  (GET "/" []
    (view-layout
      (view-create-account-input)
      (view-balance-input)
      (view-payment-input)))

  (POST "/balance" [account currency]
    (let [balance (mint/balance account currency)]
      (flash-put! :message (str "Balance for " account "'s account: " balance " " currency " IOU"))
      (redirect "/")))

  (POST "/create-account" [account]
    (do
      (mint/create-account account)
      (session-put! :account account)
      (flash-put! :message (str "Account " account " created"))
      (redirect "/")))

  (POST "/pay" [from to currency amount memo]
    (do
      (mint/pay from to currency (Integer/parseInt amount) memo)
      (flash-put! :message (str "Account " to " paid"))
      (redirect "/")))

  (ANY "/*" [path]
    (redirect "/")))

(def production?
  (= "production" (get (System/getenv) "APP_ENV")))

(def development?
  (not production?))

(def app
  (-> #'handler
    (wrap-file "public")
    (wrap-file-info)
    (wrap-request-logging)
    (wrap-if development? wrap-reload '[i2conomy.wallet i2conomy.middleware])
    (wrap-bounce-favicon)
    (wrap-exception-logging)
    (wrap-if production?  wrap-failsafe)
    (wrap-if development? wrap-stacktrace)
    ; prevent wrap-reload from resetting the sessions as well
    (wrap-stateful-session {:store (memory-store custom-session-atom)})))

