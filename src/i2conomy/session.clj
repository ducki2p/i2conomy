(ns i2conomy.session)

; Session atom outside of the reloadables of wrap-reload
(def custom-session-atom (atom {}))
