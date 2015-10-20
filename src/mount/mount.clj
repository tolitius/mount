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
    (let [s-meta (-> {:state (str `~state) :start `(fn [] (~@start))}
                     (cond-> df (assoc :stop `(fn [] (~@stop)))))]
      `(defonce ~(with-meta state (merge (meta state) s-meta))
         (~@start)))))

(defn- up [{:keys [ns name start]}]
  (intern ns (symbol name) (start)))

(defn- down [{:keys [stop]}]
  (when stop
    (stop)))

(defn- f-states [f]
  (->> (all-ns)
       (mapcat ns-interns)
       (map second)
       (filter #(:state (meta %)))
       (map (comp f meta))))

(defn start []
  (doall 
    (f-states up)))

(defn stop []
  (doall
    (f-states down)))
