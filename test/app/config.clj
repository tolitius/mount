(ns app.config
  (:require [mount :refer [defstate]]
            [clojure.edn :as edn]
            [clojure.tools.logging :refer [info]]))

(defn load-config [path]
  (info "loading config from" path)
  (if (:help (mount/args))
    (info "\n\nthis is a sample mount app to demo how to pass and read runtime arguments\n"))
  (-> path 
      slurp 
      edn/read-string))

(defstate app-config 
  :start (load-config "test/resources/config.edn"))
