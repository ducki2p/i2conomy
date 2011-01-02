(ns script.run
  (:use ring.adapter.jetty)
  (:require i2conomy.wallet))

(defn runserver []
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "8000"))]
    (run-jetty #'i2conomy.wallet/app {:port port})))

(defn -main [& args]
  (runserver))
