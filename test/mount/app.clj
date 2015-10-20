(ns mount.app
  (:require [datomic.api :as d]
            [mount.utils.datomic :refer [touch]]
            [mount.config :refer [app-config]]
            [mount.nyse :as nyse]))

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


(defn find-orders [ticker]                                  ;; can take connection as param
  (let [orders (d/q '[:find ?e :in $ ?ticker
                      :where [?e :order/symbol ?ticker]] 
                    (d/db nyse/conn) ticker)]
    (touch nyse/conn orders)))

(defn create-nyse-schema []
  (create-schema nyse/conn))
