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
        [:p "Account: " (session-get :account "Unknown")]
        (when-let [flash (flash-get :error)]
          [:p.error "Error: " flash])
        (when-let [flash (flash-get :message)]
          [:p.message "Message: " flash])
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
          [:td currency] [:td amount]])]))

(defn view-history [history]
  (html
    [:h2 "history"]
    [:table
      [:tr
        [:th "Timestamp"] [:th "From"] [:th "To"] [:th "Currency"] [:th "Amount"] [:th "Memo"]]
      (for [transfer history]
        (let [{:keys [timestamp from to amount currency memo]} transfer]
          [:tr
            [:td timestamp] [:td from] [:td to] [:td currency] [:td amount] [:td memo]]))]))

(defn view-payment-input []
  (html
    [:form {:method "post" :action "/pay"}
      [:fieldset
        [:legend "Pay"]
          [:div
            [:label "From: "] [:input {:type "text" :name "from"}]]
          [:div
            [:label "To: "] [:input {:type "text" :name "to"}]]
          [:div
            [:label "Currency: "] [:input {:type "text" :name "currency"}]]
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
        (html
          (view-payment-input)
          (view-balances (mint/balances account))
          (view-history (mint/history account))))))

  (POST "/create-account" [account]
    (try
      (mint/create-account account)
      (session-put! :account account)
      (flash-put! :message (str "Account " account " created"))
      (catch IllegalArgumentException e
        (flash-put! :error (.getMessage e))))
    (redirect "/"))

  (POST "/pay" [from to currency amount memo]
    (try
      (mint/pay from to currency (Integer/parseInt amount) memo)
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

