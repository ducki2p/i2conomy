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
        [:h1 "I2Conomy"]
        [:p "Account: " (h (session-get :account "Unknown"))]
        (when-let [flash (flash-get :error)]
          [:p.error "Error: " (h flash)])
        (when-let [flash (flash-get :message)]
          [:p.message "Message: " (h flash)])
        content])))

(defn view-create-account-input []
  (html
    [:form {:method "post" :action "/create-account"}
      [:fieldset
        [:legend "Create Account"]
        [:div
          [:label {:for "account"} "Account: "]
          [:input {:type "text" :id "account" :name "account"}]]]
      [:div.button
        [:input {:type "submit" :value "create"}]]]))

(defn view-balances [balances]
  (html
    [:h2 "balances"]
    [:table
      [:tr
        [:th "Currency"] [:th "Amount"]]
      (for [[currency amount] balances]
        [:tr
          [:td (h currency)] [:td (h amount)]])]))

(defn view-history [history]
  (html
    [:h2 "history"]
    [:table
      [:tr
        [:th "Timestamp"] [:th "From"] [:th "To"] [:th "Currency"] [:th "Amount"] [:th "Memo"]]
      (for [transfer history]
        (let [{:keys [timestamp from to amount currency memo]} transfer]
          [:tr
            [:td timestamp] [:td (h from)] [:td (h to)]
            [:td (h currency)] [:td amount] [:td (h memo)]]))]))

(defn input-currency-dropdown [account balances]
  (html
    [:select {:name "currency"}
      [:option {:value (h account) :selected "selected"} (h account)]
      (for [[currency amount] balances
            :when (not= account currency)]
        [:option {:value (h currency)} (h currency) " (" amount ")"])]))

(defn view-payment-input [account balances]
  (html
    [:form {:method "post" :action "/pay"}
      [:fieldset
        [:legend "Pay"]
          [:div
            [:label "To: "] [:input {:type "text" :name "to"}]]
          [:div
            [:label "Currency: "]
            (input-currency-dropdown account balances)]
          [:div
            [:label "Amount: "] [:input {:type "text" :name "amount"}]]
          [:div
            [:label "Memo: "] [:input {:type "text" :name "memo"}]]]
        [:div.button
          [:input {:type "submit" :value "pay"}]]]))

(defroutes handler
  (GET "/" []
    (view-layout
      (view-create-account-input)
      (when-let [account (session-get :account)]
        (let [balances (mint/balances account)
              history (mint/history account)]
          (html
            (view-payment-input account balances)
            (view-balances balances)
            (view-history history))))))

  (POST "/create-account" [account]
    (try
      (mint/create-account account)
      (flash-put! :message (str "Account " account " created"))
      (catch IllegalArgumentException e
        (flash-put! :error (.getMessage e))))
    ; XXX cheat here to allow user switching
    (session-put! :account account)
    (redirect "/"))

  (POST "/pay" [to currency amount memo]
    (try
      (let [account (session-get :account)
            amount (Integer/parseInt amount)]
        (mint/pay account to currency amount memo))
      (flash-put! :message (str "Account " to " paid"))
      (catch NumberFormatException _
        (flash-put! :error "Invalid amount"))
      (catch IllegalArgumentException e
        (flash-put! :error (.getMessage e))))
    (redirect "/"))

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

