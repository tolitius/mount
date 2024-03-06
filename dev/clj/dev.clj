(ns dev
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.namespace.repl :as tn]
            [mount.core :as mount :refer [defstate]]
            [mount.tools.graph :refer [states-with-deps]]
            [app.utils.logging :refer [with-logging-status]]
            [app.www]
            [app.db :refer [conn]]
            [app.example]
            [app.nyse :refer [find-orders add-order]]))  ;; <<<< replace this your "app" namespace(s) you want to be available at REPL time

(defn start []
  (with-logging-status)
  (mount/start #'app.conf/config
               #'app.db/conn
               #'app.www/nyse-app
               #'app.example/nrepl))             ;; example on how to start app with certain states

(defn stop []
  (mount/stop))

(defn refresh []
  (stop)
  (tn/refresh))

(defn refresh-all []
  (stop)
  (tn/refresh-all))

(defn go
  "starts all states defined by defstate"
  []
  (start)
  :ready)

(defn reset
  "stops all states defined by defstate, reloads modified source files, and restarts the states"
  []
  (stop)
  (tn/refresh :after 'dev/go))

(mount/in-clj-mode)

(defn load-data-readers!
  "Refresh *data-readers* with readers from newly acquired dependencies."
  []
  (#'clojure.core/load-data-readers)
  (set! *data-readers* (.getRawRoot #'*data-readers*)))

(load-data-readers!)
