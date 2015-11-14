(ns app.nyse
  (:require [mount :refer [defstate]]
            [datomic.api :as d]
            [clojure.tools.logging :refer [info]]
            [app.config :refer [app-config]]))

(defn- new-connection [{:keys [datomic-uri]}]
  (info "creating a connection to datomic:" datomic-uri)
  (d/create-database datomic-uri)
  (d/connect datomic-uri))

(defn disconnect [{:keys [datomic-uri]} conn]
  (info "disconnecting from " datomic-uri)
  (.release conn)                        ;; usually it's not released, here just to illustrate the access to connection on (stop)
  (d/delete-database datomic-uri))

(defstate conn :start (new-connection (mount/args))
               :stop (disconnect (mount/args) conn))
