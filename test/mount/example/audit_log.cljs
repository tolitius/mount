(ns mount.example.audit-log
  (:require [datascript.core :as d])
  (:require-macros [mount.core :refer [defstate]]))

(defstate log :start (d/create-conn {}))

(defn audit [db source & msg]
  (d/transact! @db [{:db/id -1
                     :source source
                     :timestamp (js/Date.)
                     :msg (apply str msg)}]))

(defn find-source-logs [db source]
  (d/q '{:find [?t ?msg]
         :in [$ ?s] 
         :where [[?e :source ?s]
                 [?e :timestamp ?t]
                 [?e :msg ?msg]]}
       @@db source))

(defn find-all-logs [db]
  (->> (map :e (d/datoms @@db :aevt :timestamp)) 
       dedupe
       (d/pull-many @@db '[:timestamp :source :msg])))
