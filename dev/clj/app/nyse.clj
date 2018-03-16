(ns app.nyse
  (:require [datomic.api :as d]
            [app.utils.datomic :refer [touch]]))

(defn add-order [conn {:keys [ticker bid offer qty]}]
  @(d/transact conn [{:db/id (d/tempid :db.part/user)
                      :order/symbol ticker
                      :order/bid bid
                      :order/offer offer
                      :order/qty qty}]))

(defn find-orders [conn ticker]
  (let [orders (d/q '[:find ?e :in $ ?ticker
                      :where [?e :order/symbol ?ticker]]
                    (d/db conn) ticker)]
    (touch conn orders)))
