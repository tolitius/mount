(ns tapp.utils.datomic
  (:require [datomic.api :as d]))

(alter-meta! *ns* assoc ::load false)

(defn entity [conn id]
  (d/entity (d/db conn) id))

(defn touch
  "takes 'entity ids' results from a query
    e.g. '#{[272678883689461] [272678883689462] [272678883689459] [272678883689457]}'"
  [conn results]
  (let [e (partial entity conn)]
    (map #(-> % first e d/touch) results)))
