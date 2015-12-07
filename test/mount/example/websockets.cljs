(ns mount.example.websockets
  (:require [mount.example.app-config :refer [config]]
            [mount.example.audit-log :refer [audit log]])
  (:require-macros [mount.core :refer [defstate]]))

(defn connect [uri]
  (audit log :system-a "connecting to '" uri "'")
  (let [ws (js/WebSocket. uri)]
    (set! (.-onopen ws) #(audit log :system-a "opening ws @" uri))
    (set! (.-onclose ws) #(audit log :system-a "closing ws @" uri))
    ws))

(defn disconnect [ws]
  (audit log "disconnecting " @ws)
  (.close @ws))

(defstate system-a :start (connect (get-in @config [:system-a :uri]))
                   :stop (disconnect system-a))
