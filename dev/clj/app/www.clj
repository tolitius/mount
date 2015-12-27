(ns app.www
  (:require [app.nyse :refer [add-order find-orders create-nyse-schema]]
            [mount.core :refer [defstate]]
            [cheshire.core :refer [generate-string]]
            [compojure.core :refer [routes defroutes GET]]))

(defroutes mount-example-routes

  (GET "/" [] "welcome to mount sample app!")

  (GET "/nyse/orders/:ticker" [ticker]
       (generate-string (find-orders ticker))))

(defstate nyse-app :start (do (create-nyse-schema)
                              (add-order "GOOG" 665.51M 665.59M 100)))
