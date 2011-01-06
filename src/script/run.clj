(ns script.run
  (:require [clojure.contrib.sql :as sql])
  (:use ring.adapter.jetty)
  (:require i2conomy.wallet)
  (:require [i2conomy.db :as db]))

(defn runserver []
  (sql/with-connection db/file-db
    (db/create-tables)
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "8000"))]
    (run-jetty #'i2conomy.wallet/app {:port port}))))

(defn -main [& args]
  (runserver))
