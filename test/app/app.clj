(ns app
  (:require [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [mount.core :as mount :refer [defstate]]
            [app.config :refer [app-config]]
            [app.www])
  (:gen-class))         ;; for -main / uberjar (no need in dev)

;; example on creating a network REPL
(defn- start-nrepl [{:keys [host port]}]
  (start-server :bind host :port port))

;; nREPL is just another simple state
(defstate nrepl :start (start-nrepl (:nrepl app-config))
                :stop (stop-server nrepl))

;; example of an app entry point
(defn -main [& args]
  (mount/start))
