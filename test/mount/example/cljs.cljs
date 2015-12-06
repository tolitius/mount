(ns mount.example.cljs
  (:require [mount.core :as mount])
  (:require-macros [mount.core :refer [defstate]]))

(enable-console-print!)

(println "hi from mount!")

(defn on-js-reload []
  "reloading js..")
