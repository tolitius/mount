(ns mount.example.cljs
  (:require [mount.core :as mount]
            [mount.example.websockets :refer [system-a]]))

(enable-console-print!)

(println "(mount/start)" (mount/start))

(println "system-a: " @system-a)

;; time for websocket to connect
(js/setTimeout #(println "(mount/stop)" (mount/stop))
               500)
