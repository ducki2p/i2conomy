(defproject i2conomy "0.0.1"
  :description "IOU based distributed currency for I2P"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [ring/ring-core "0.3.5"]
                 [ring/ring-devel "0.3.5"]
                 [ring/ring-jetty-adapter "0.3.5"]
                 [compojure "0.5.3"]
                 [hiccup "0.3.1"]
                 [sandbar/sandbar "0.3.0"]
                 [org.jasypt/jasypt "1.7"]
                 [org.bituf/clj-dbcp "0.4"]
                 [org.xerial/sqlite-jdbc "3.7.2"]]
  :dev-dependencies [[org.clojars.cais/lein-run "1.0.1-SNAPSHOT"]]
  :main script.run)
