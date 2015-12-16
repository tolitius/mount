(ns tapp.nyse
  (:require [mount.core :as mount :refer [defstate]]
            [datomic.api :as d]
            [clojure.tools.logging :refer [info]]
            [tapp.conf :refer [config]]))

(alter-meta! *ns* assoc ::load false)

(defn- new-connection [conf]
  (info "conf: " conf)
  (let [uri (get-in @conf [:datomic :uri])]
    (info "creating a connection to datomic:" uri)
    (d/create-database uri)
    (d/connect uri)))

(defn disconnect [conf conn]
  (let [uri (get-in @conf [:datomic :uri])]
    (info "disconnecting from " uri)
    (.release @conn)                         ;; usually it's not released, here just to illustrate the access to connection on (stop)
    (d/delete-database uri)))

(defstate conn :start (new-connection config)
               :stop (disconnect config conn))
