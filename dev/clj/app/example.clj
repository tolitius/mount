(ns app.example
  (:require [datomic.api :as d]
            [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [mount.core :as mount :refer [defstate]]
            [app.utils.datomic :refer [touch]]
            [app.conf :refer [config]]
            [app.nyse :as nyse]))

;; example on creating a network REPL
(defn- start-nrepl [{:keys [host port]}]
  (start-server :bind host :port port))

;; nREPL is just another simple state
(defstate nrepl :start (start-nrepl (:nrepl config))
                :stop (stop-server nrepl))

;; datomic schema
(defn create-schema [conn]
  (let [schema [{:db/id #db/id [:db.part/db]
                 :db/ident :order/symbol
                 :db/valueType :db.type/string
                 :db/cardinality :db.cardinality/one
                 :db/index true
                 :db.install/_attribute :db.part/db}

                {:db/id #db/id [:db.part/db]
                 :db/ident :order/bid
                 :db/valueType :db.type/bigdec
                 :db/cardinality :db.cardinality/one
                 :db.install/_attribute :db.part/db}
                
                {:db/id #db/id [:db.part/db]
                 :db/ident :order/qty
                 :db/valueType :db.type/long
                 :db/cardinality :db.cardinality/one
                 :db.install/_attribute :db.part/db}

                {:db/id #db/id [:db.part/db]
                 :db/ident :order/offer
                 :db/valueType :db.type/bigdec
                 :db/cardinality :db.cardinality/one
                 :db.install/_attribute :db.part/db}]]

        @(d/transact conn schema)))

(defn add-order [ticker bid offer qty]                       ;; can take connection as param
  @(d/transact nyse/conn [{:db/id (d/tempid :db.part/user)
                           :order/symbol ticker
                           :order/bid bid
                           :order/offer offer
                           :order/qty qty}]))


(defn find-orders [ticker]                                   ;; can take connection as param
  (let [orders (d/q '[:find ?e :in $ ?ticker
                      :where [?e :order/symbol ?ticker]] 
                    (d/db nyse/conn) ticker)]
    (touch nyse/conn orders)))

(defn create-nyse-schema []
  (create-schema nyse/conn))

;; example of an app entry point
(defn -main [& args]
  (mount/start))
