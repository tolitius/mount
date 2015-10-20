(ns mount.nyse
  (:require [mount :refer [defstate]]
            [mount.config :refer [app-config]]
            [datomic.api :as d]
            [clojure.tools.logging :refer [info]]))

(defn- new-connection [conf]
  (info "conf: " conf)
  (let [uri (get-in conf [:datomic :uri])]
    (info "creating a connection to datomic:" uri)
    (d/create-database uri)
    (d/connect uri)))

(defn disconnect [conf conn]
  (let [uri (get-in conf [:datomic :uri])]
    (info "disconnecting from " uri)
    (.release conn)                        ;; usually it's not released, here just to illustrate the access to connection on (stop)
    (d/delete-database uri)))

(defstate conn :start (new-connection app-config)
               :stop (disconnect app-config conn))
