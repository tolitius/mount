(ns app.config
  (:require [mount.core :refer [defstate]]
            [clojure.edn :as edn]
            [clojure.tools.logging :refer [info]]))

(defn load-config [path]
  (info "loading config from" path)
  (-> path 
      slurp 
      edn/read-string))

(defstate app-config 
  :start #(load-config "test/resources/config.edn"))
