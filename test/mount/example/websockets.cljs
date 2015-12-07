(ns mount.example.websockets
  (:require [mount.example.app-config :refer [config]])
  (:require-macros [mount.core :refer [defstate]]))

(defn connect [uri]
  (println "connecting to " uri)
  (let [ws (js/WebSocket. uri)]
    (set! (.-onopen ws) #(println "opening ws @" uri))
    (set! (.-onclose ws) #(println "closing ws @" uri))
    ws))

(defn disconnect [ws]
  (println "disconnecting " @ws)
  (.close @ws))

(defstate system-a :start (connect (get-in @config [:system-a :uri]))
                   :stop (disconnect system-a))
