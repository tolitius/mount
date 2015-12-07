(ns mount.example.app
  (:require [mount.core :as mount]
            [mount.example.app-config]
            [mount.example.websockets]
            [mount.example.audit-log :refer [log find-all-logs]]))

(defn show-log []
  (.write js/document 
          (interpose "<br/>" (find-all-logs log))))

(mount/start)

;; time for websocket to connect
(js/setTimeout #(do (mount/stop-except "#'mount.example.audit-log/log")
                    (show-log)) 500)

