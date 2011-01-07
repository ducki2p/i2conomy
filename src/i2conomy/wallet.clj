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

(defn view-menubar []
  (html
    [:div#app-menu.span-7.nav-menu
      [:ul.nav-menu
        [:li (link-to "/" "Wallet")]
        [:li (link-to "#" "Contacts")]
        [:li (link-to "#" "Market")]]]
    [:div#my-address.span-4.prepend-1
      [:input {:type "text" :value (h (current-username))}]]
    [:div#account-menu.span-7.last.nav-menu
      [:ul.nav-menu
        [:li (link-to "#" "Settings")]
        [:li (link-to "#" "Help")]
        [:li (link-to "/logout" "Sign out")]]]))

(defn view-flash-message []
  (html
    [:div#message.span-24.last
      (for [type [:error :info :success]
            :let [msg (flash-get type)]
            :when msg]
        [:p {:class type} (h msg)])]))

(defn view-layout [& content]
  (html
    (doctype :xhtml-strict)
    (xhtml-tag "en"
      [:head
        [:meta {:http-equiv "Content-type"
                :content "text/html; charset=utf-8"}]
        [:title "I2Conomy"]
        [:link {:href "css/screen.css" :rel "stylesheet" :type "text/css" :media "screen, projection"}]
        [:link {:href "css/print.css" :rel "stylesheet" :type "text/css" :media "print"}]
        "<!--[if lt IE 8]>"
        [:link {:href "css/ie.css" :rel "stylesheet" :type "text/css" :media "screen, projection"}]
        "<![endif]-->"
        [:link {:href "css/menu.css" :rel "stylesheet" :type "text/css" :media "screen, projection"}]]
      [:body
        [:div.container
          [:div#header.span-24.last
            [:div#title.span-5
              [:h1 "I2Conomy"]]
            (when (any-role-granted? :user)
              (view-menubar))]
          [:div#content.clear.span-24.last
            (view-flash-message)
            content]]])))

(defn view-login []
  (html
    [:div#login.clear.span-12.last
      [:form {:method "post" :action "/login"}
        [:fieldset
          [:legend "Sign in"]
          [:p
            [:label {:for "username"} "Username: "] [:br]
            [:input.text {:type "text" :id "username" :name "username"}]]
          [:p
            [:label {:for "password"} "Password: "] [:br]
            [:input.text {:type "password" :id "password" :name "password"}]]
          [:p.button
            [:input {:type "submit" :value "Sign in"}]
            " or "
            (link-to "/register" "Create an account")]]]]))

(defn view-register []
  (html
    [:div#register.clear.span-12.last
      [:form {:method "post" :action "/register"}
        [:fieldset
          [:legend "Create an account"]
          [:p
            [:label {:for "username"} "Username: "] [:br]
            [:input.text {:type "text" :id "username" :name "username"}]]
          [:p
            [:label {:for "password"} "Password: "] [:br]
            [:input.text {:type "password" :id "password" :name "password"}]]
          [:p.button
            [:input {:type "submit" :value "Create an account"}]]]]]))

(defn view-balances [balances]
  (html
    [:div#balance.clear.span-12
      [:table {:style "border: 1px grey solid"}
        [:tr
          [:th "IOU Balance"] [:th "Amount"]]
        (for [{:keys [currency amount]} balances]
          [:tr
            [:td (h currency)] [:td (h amount)]])]
      [:p (link-to "#" "Show all")]]))

(defn format-date [date]
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss") date))

(defn view-history [username history]
  (html
    [:div#transaction-history.clear.span-24.last
      [:div#transaction-header.span-17
        [:span {:style "font-size: 14pt"}
          "Transaction history (last 10)"]]
      [:div#transaction-pagination.span-7.last
        "&lt;&lt; Newer | " (link-to "#" "Older &gt;&gt;")]
      [:div#transaction-table.clear.span-24.last
        [:table
          [:tr
            [:th "Date"] [:th "From / To"] [:th "IOU"] [:th "Amount"] [:th "Memo"]]
          (for [{:keys [timestamp from to amount currency memo]} history
                :let [from-to (if (= to username ) from (str "To: " to))
                     amount-rel (if (= to username ) amount (- amount))]]
            [:tr
              [:td (format-date timestamp)] [:td (h from-to)]
              [:td (h currency)] [:td amount-rel] [:td (h memo)]])]]]))

(defn input-currency-dropdown [username balances]
  (html
    [:select.text {:name "currency"}
      [:option {:value (h username) :selected "selected"} (h username)]
      (for [{:keys [currency amount]} balances
            :when (not= username currency)]
        [:option {:value (h currency)} (h currency) " (" amount ")"])]))

(defn view-payment-form [username balances]
  (html
    [:div#pay.span-12.last
      [:form#pay-form {:method "post" :action "/pay"}
        [:fieldset {:style "background-color: #ddd;"}
          [:legend "Pay"]
            [:p
              [:label "To: "] [:br]
              [:input.text {:type "text" :name "to"}]]
            [:p
              [:label "IOU: "] [:br]
              (input-currency-dropdown username balances)]
            [:p
              [:label "Amount: "] [:br]
              [:input.text {:type "text" :name "amount"}]]
            [:p
              [:label "Memo: "] [:br]
              [:input.text {:type "text" :name "memo"}]]
            [:p.button
              [:input {:type "submit" :value "Pay"}]]]]]))

(defroutes handler
  (GET "/" []
    (view-layout
      (if-let [username (current-username)]
        (let [balances (mint/balances username)
              history (mint/history username)]
          (html
            (view-balances balances)
            (view-payment-form username balances)
            (view-history username history)))
        (view-login))))

  (GET "/login" []
    (view-layout
      (view-login)))

  (POST "/login" [username password]
    (if (mint/valid-login? username password)
      (session-put! :current-user {:name username :roles #{:user}})
      (flash-put! :error "Invalid username and/or password"))
    (redirect "/"))

  (GET "/register" []
    (view-layout
      (view-register)))

  (POST "/register" [username password]
    (try
      (mint/create-account username password)
      (flash-put! :info (str "Account " username " created"))
      (session-put! :current-user {:name username :roles #{:user}})
      (redirect "/")
      (catch IllegalArgumentException e
        (flash-put! :error (.getMessage e))
        (view-layout
          (view-register)))))

  (POST "/pay" [to currency amount memo]
    (try
      (let [from (current-username)
            amount (Integer/parseInt amount)]
        (mint/pay from to currency amount memo))
      (flash-put! :success (str "Account " to " paid"))
      (catch NumberFormatException _
        (flash-put! :error "Invalid amount"))
      (catch IllegalArgumentException e
        (flash-put! :error (.getMessage e))))
    (redirect "/"))

  (ANY "/logout*" []
    (flash-put! :info "You are signed out")
    (logout! {}))

  (GET "/permission-denied" []
    (flash-put! :error "Permission denied, please login")
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
      #"/register.*"          :guest
      #"/permission-denied.*" :any
      #".*\.(css|ico|js|png|gif)" :any
      #".*"                   :user])

; XXX this isn't used correctly, I think
(defn authenticator [request]
  (if-let [user (current-user)]
    {:name user :roles #{:user}}
    {:name :anonymous :roles #{:guest}}))

(def app
  (-> #'handler
    (with-db)
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

