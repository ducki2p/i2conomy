(defproject i2conomy "0.0.1"
  :description "IOU based distributed currency for I2P"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [ring/ring-core "0.3.5"]
                 [ring/ring-devel "0.3.5"]
                 [ring/ring-jetty-adapter "0.3.5"]
                 [compojure "0.5.3"]
                 [hiccup "0.3.1"]
                 [sandbar/sandbar "0.3.0"]]
  :dev-dependencies [[lein-run "1.0.1-SNAPSHOT"]]
  :main script.run)
