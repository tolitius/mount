(ns dev
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.namespace.repl :as tn]
            [boot.core :refer [load-data-readers!]]
            [mount.core :as mount]
            [app.utils.logging :refer [with-logging-status]]
            [app.www]
            [app.example]
            [app.nyse :refer [create-nyse-schema find-orders add-order]]))  ;; <<<< replace this your "app" namespace(s) you want to be available at REPL time

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
(load-data-readers!)
