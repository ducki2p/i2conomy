(ns i2conomy.session)

(def custom-session-atom
  "Session atom outside of the reloadables of wrap-reload"
  (atom {}))
