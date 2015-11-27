(ns app.www
  (:require [app.nyse :refer [add-order find-orders create-nyse-schema]]
            [app.config :refer [app-config]]
            [mount.core :refer [defstate]]
            [cheshire.core :refer [generate-string]]
            [compojure.core :refer [routes defroutes GET POST]]
            [compojure.handler :as handler]
            [ring.adapter.jetty :refer [run-jetty]]))

(defroutes mount-example-routes

  (GET "/" [] "welcome to mount sample app!")
  (GET "/nyse/orders/:ticker" [ticker]
       (generate-string (find-orders ticker)))

  (POST "/nyse/orders" [ticker qty bid offer] 
        (add-order ticker (bigdec bid) (bigdec offer) (Integer/parseInt qty))
        (generate-string {:added {:ticker ticker 
                                  :qty qty 
                                  :bid bid 
                                  :offer offer}})))

(defn start-nyse [{:keys [www]}]
  (create-nyse-schema)              ;; creating schema (usually done long before the app is started..)
  (-> (routes mount-example-routes)
      (handler/site)
      (run-jetty {:join? false
                  :port (:port www)})))

(defstate nyse-app :start #(start-nyse app-config)
                   :stop #(.stop nyse-app))  ;; it's a "org.eclipse.jetty.server.Server" at this point
