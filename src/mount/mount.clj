(ns mount
  (:require [clojure.tools.macro :as macro]
            [clojure.tools.namespace.repl :refer [disable-reload!]]
            [clojure.tools.logging :refer [info]]))

(disable-reload!)

(defonce ^:private session-id (System/currentTimeMillis))
(defonce ^:private state-seq (atom 0))
(defonce ^:private state-order (atom {}))

(defn- make-state-seq [state]
  (or (@state-order state)
      (let [nseq (swap! state-seq inc)]
        (swap! state-order assoc state nseq)
        nseq)))

;;TODO validate stop and the fact that start and stop are fns
(defn- validate [{:keys [start stop]}]
  (when-not start 
    (throw (IllegalArgumentException. "can't start a stateful thing without a start function. (i.e. missing :start fn)")))
  {:start start :stop stop})

(defmacro defstate [state & body]
  (info "calling defstate: " state ", body: " body)
  (let [[state [c cf d df]] (macro/name-with-attributes state body)
        {:keys [start stop]} (validate {c cf d df})]
    (let [s-meta (-> {:session-id session-id
                      :order (make-state-seq state)
                      :start `(fn [] (~@start)) 
                      :started? true}
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
       (filter #(= (:session-id (meta %)) session-id))
       (map #(f % (meta %)))))

(defn start []
  (doall 
    (f-states up)))

(defn stop []
  (doall
    (f-states down)))
