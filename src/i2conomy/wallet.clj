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
        sandbar.auth
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
        (when (any-role-granted? :user)
          [:div
            [:p "Account: " (h (current-username))]
            [:p (link-to "/logout" "Logout")]])
        (when-let [flash (flash-get :error)]
          [:p.error "Error: " (h flash)])
        (when-let [flash (flash-get :message)]
          [:p.message "Message: " (h flash)])
        content])))

(defn view-login []
  (html
    [:form {:method "post" :action "/login"}
      [:fieldset
        [:legend "Login"]
        [:div
          [:label {:for "username"} "Username: "]
          [:input {:type "text" :id "username" :name "username"}]]
        [:div
          [:label {:for "password"} "Password: "]
          [:input {:type "password" :id "password" :name "password"}]]]
      [:div.button
        [:input {:type "submit" :value "login"}]]
      [:p (link-to "/signup" "Signup")]]))

(defn view-signup []
  (html
    [:form {:method "post" :action "/signup"}
      [:fieldset
        [:legend "Signup"]
        [:div
          [:label {:for "username"} "Username: "]
          [:input {:type "text" :id "username" :name "username"}]]
        [:div
          [:label {:for "password"} "Password: "]
          [:input {:type "password" :id "password" :name "password"}]]]
      [:div.button
        [:input {:type "submit" :value "signup"}]]]))

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

(defn input-currency-dropdown [username balances]
  (html
    [:select {:name "currency"}
      [:option {:value (h username) :selected "selected"} (h username)]
      (for [[currency amount] balances
            :when (not= username currency)]
        [:option {:value (h currency)} (h currency) " (" amount ")"])]))

(defn view-payment-input [username balances]
  (html
    [:form {:method "post" :action "/pay"}
      [:fieldset
        [:legend "Pay"]
          [:div
            [:label "To: "] [:input {:type "text" :name "to"}]]
          [:div
            [:label "Currency: "]
            (input-currency-dropdown username balances)]
          [:div
            [:label "Amount: "] [:input {:type "text" :name "amount"}]]
          [:div
            [:label "Memo: "] [:input {:type "text" :name "memo"}]]]
        [:div.button
          [:input {:type "submit" :value "pay"}]]]))

(defroutes handler
  (GET "/" []
    (view-layout
      (if-let [username (current-username)]
        (let [balances (mint/balances username)
              history (mint/history username)]
          (html
            (view-payment-input username balances)
            (view-balances balances)
            (view-history history)))
        (view-login))))

  (GET "/login" []
    (view-layout
      (view-login)))

  (POST "/login" [username password]
    (if (mint/valid-login? username password)
      (session-put! :current-user {:name username :roles #{:user}})
      (flash-put! :error "Invalid username and/or password"))
    (redirect "/"))

  (GET "/signup" []
    (view-layout
      (view-signup)))

  (POST "/signup" [username password]
    (try
      (mint/create-account username password)
      (flash-put! :message (str "Account " username " created"))
      (session-put! :current-user {:name username :roles #{:user}})
      (redirect "/")
      (catch IllegalArgumentException e
        (flash-put! :error (.getMessage e))
        (view-layout
          (view-signup)))))

  (POST "/pay" [to currency amount memo]
    (try
      (let [from (current-username)
            amount (Integer/parseInt amount)]
        (mint/pay from to currency amount memo))
      (flash-put! :message (str "Account " to " paid"))
      (catch NumberFormatException _
        (flash-put! :error "Invalid amount"))
      (catch IllegalArgumentException e
        (flash-put! :error (.getMessage e))))
    (redirect "/"))

  (ANY "/logout*" [] (logout! {}))

  (GET "/permission-denied" []
    (flash-put! :error "Permission denied, please login ")
    (view-layout
      (view-login)))

  (ANY "/*" [path]
    (redirect "/")))

(def production?
  (= "production" (get (System/getenv) "APP_ENV")))

(def development?
  (not production?))

(def security-policy
     [#"/"                    :any
      #"/login.*"             :any
      #"/logout.*"            :any
      #"/signup.*"            :guest
      #"/permission-denied.*" :any
      #".*\.(css|js|png|gif)" :any
      #".*"                   :user])

(defn authenticator [request]
  (if-let [user (current-user)]
    {:name user :roles #{:user}}
    {:name :anonymous :roles #{:guest}}))

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
    (with-security security-policy authenticator)
    ; prevent wrap-reload from resetting the sessions as well
    (wrap-stateful-session {:store (memory-store custom-session-atom)})))

