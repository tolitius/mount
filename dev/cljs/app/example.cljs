(ns app.example
  (:require [mount.core :as mount]
            [app.conf]
            [app.websockets]
            [app.audit-log :refer [log find-all-logs]]
            [cljs-time.format :refer [unparse formatters]]
            [hiccups.runtime :as hiccupsrt])
  (:require-macros [hiccups.core :as hiccups :refer [html]]))

(defn format-log-event [{:keys [timestamp source msg]}]
  (str (unparse (formatters :date-hour-minute-second-fraction) timestamp)
                " &#8594; [" (name source) "]: " msg))

(defn show-log []
  (.write js/document 
    (html [:ul (doall (for [e (find-all-logs log)]
                 [:li (format-log-event e)]))])))

(mount/start)

;; time to establish a websocket connection before disconnecting
;; (js/setTimeout #(mount/stop-except "#'app.audit-log/log") 500)

;; time to close a connection to show it in audit
(js/setTimeout #(show-log) 1000)

