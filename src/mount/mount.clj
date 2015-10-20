(ns mount
  (:require [clojure.tools.macro :as macro]
            [clojure.tools.logging :refer [info]]))

;;TODO validate stop and the fact that start and stop are fns
(defn- validate [{:keys [start stop]}]
  (when-not start 
    (throw (IllegalArgumentException. "can't start a stateful thing without a start function. (i.e. missing :start fn)")))
  {:start start :stop stop})

(defmacro defstate [state & body]
  (let [[state [c cf d df]] (macro/name-with-attributes state body)
        {:keys [start stop]} (validate {c cf d df})]
    (let [s-meta (-> {:state (str `~state) :start `(fn [] (~@start)) :started? true}
                     (cond-> df (assoc :stop `(fn [] (~@stop)))))]
      `(defonce ~(with-meta state (merge (meta state) s-meta))
         (~@start)))))

(defn- up [var {:keys [ns name start started?]}]
  (when-not started?
    (intern ns (symbol name) (start))
    (alter-meta! var assoc :started? true)))

(defn- down [var {:keys [stop started?]}]
  (when started?
    (alter-meta! var assoc :started? false)
    (when stop (stop))))

(defn- f-states [f]
  (->> (all-ns)
       (mapcat ns-interns)
       (map second)
       (filter #(:state (meta %)))
       (map #(f % (meta %)))))

(defn start []
  (doall 
    (f-states up)))

(defn stop []
  (doall
    (f-states down)))
