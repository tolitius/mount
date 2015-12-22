(ns app.nyse
  (:require [datomic.api :as d]
            [app.db :refer [create-schema] :as db]
            [app.utils.datomic :refer [touch]]))

(defn add-order [ticker bid offer qty]                       ;; can take connection as param
  @(d/transact db/conn [{:db/id (d/tempid :db.part/user)
                         :order/symbol ticker
                         :order/bid bid
                         :order/offer offer
                         :order/qty qty}]))

(defn find-orders [ticker]                                   ;; can take connection as param
  (let [orders (d/q '[:find ?e :in $ ?ticker
                      :where [?e :order/symbol ?ticker]] 
                    (d/db db/conn) ticker)]
    (touch db/conn orders)))

(defn create-nyse-schema []
  (create-schema db/conn))
