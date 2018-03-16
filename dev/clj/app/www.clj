(ns app.www
  (:require [app.nyse :refer [add-order find-orders]]
            [app.db :refer [conn create-schema]]
            [app.conf :refer [config]]
            [mount.core :refer [defstate]]
            [cheshire.core :refer [generate-string]]
            [compojure.core :refer [routes defroutes GET POST]]
            [compojure.handler :as handler]
            [ring.adapter.jetty :refer [run-jetty]]))

(defroutes mount-example-routes

  (GET "/" [] "welcome to mount sample app!")
  (GET "/nyse/orders/:ticker" [ticker]
       (generate-string (find-orders conn ticker)))

  (POST "/nyse/orders" [ticker qty bid offer]
        (let [order {:ticker ticker
                     :bid (bigdec bid)
                     :offer (bigdec offer)
                     :qty (Integer/parseInt qty)}]
          (add-order conn order)
          (generate-string {:added order}))))

(defn start-nyse [conn {:keys [www]}]     ;; app entry point
  (create-schema conn)                    ;; just an example, usually schema would already be there
  (-> (routes mount-example-routes)
      (handler/site)
      (run-jetty {:join? false
                  :port (:port www)})))

(defstate nyse-app :start (start-nyse conn config)
                   :stop (.stop nyse-app))  ;; it's a "org.eclipse.jetty.server.Server" at this point
