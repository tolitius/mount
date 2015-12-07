(ns mount.example.app-config
  (:require-macros [mount.core :refer [defstate]]))

(defn load-config [path]
  (println "loading config from " path " (at least pretending)")
  {:system-a {:uri "ws://echo.websocket.org/"}})

(defstate config :start (load-config "resources/config.end"))
