(ns tapp.conf
  (:require [mount.core :as mount :refer [defstate]]
            [clojure.edn :as edn]
            [clojure.tools.logging :refer [info]]))

(alter-meta! *ns* assoc ::load false)

(defn load-config [path]
  (info "loading config from" path)
  (-> path
      slurp
      edn/read-string))

(defstate config
  :start (load-config "dev/resources/config.edn"))
