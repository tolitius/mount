(ns mount.example.app-config
  (:require [mount.example.audit-log :refer [audit log]])
  (:require-macros [mount.core :refer [defstate]]))

(defn load-config [path]
  (audit log :app-config "loading config from '" path "' (at least pretending)")
  {:system-a {:uri "ws://echo.websocket.org/"}})

(defstate config :start (load-config "resources/config.end"))
