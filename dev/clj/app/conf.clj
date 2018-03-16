(ns app.conf
  (:require [mount.core :as mount :refer [defstate]]
            [clojure.edn :as edn]
            [clojure.tools.logging :refer [info]]))

(defn load-config [path]
  (info "loading config from" path)
  (-> path
      slurp
      edn/read-string))

(defstate config
  :start (load-config "dev/resources/config.edn"))
