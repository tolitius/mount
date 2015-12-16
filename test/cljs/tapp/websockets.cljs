(ns tapp.websockets
  (:require [tapp.conf :refer [config]]
            [tapp.audit-log :refer [audit log]])
  (:require-macros [mount.core :refer [defstate]]))

(defn ws-status [ws]
  {:url (.-url ws) :ready-state (.-readyState ws)})

(defn connect [uri]
  (let [ws (js/WebSocket. uri)]
    (audit log :system-a "connecting to " (ws-status ws))
    (set! (.-onopen ws) #(audit log :system-a "opened " (ws-status ws)))
    (set! (.-onclose ws) #(audit log :system-a "closed " (ws-status ws)))
    ws))

(defn disconnect [ws]
  (audit log :system-a "closing " (ws-status @ws))
  (.close @ws)
  (audit log :system-a "disconnecting " (ws-status @ws)))

(defstate system-a :start (connect (get-in @config [:system-a :uri]))
                   :stop (disconnect system-a))
