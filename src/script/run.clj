(ns script.run
  (:use ring.adapter.jetty)
  (:require i2conomy.wallet))

(defn -main [& args]
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "8000"))]
    (run-jetty #'i2conomy.wallet/app {:port port})))
