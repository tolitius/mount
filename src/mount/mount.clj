(ns mount
  (:require [clojure.tools.macro :as macro]
            [clojure.tools.namespace.repl :refer [disable-reload!]]
            [clojure.tools.logging :refer [info debug error]]))

(disable-reload!)

;; (defonce ^:private session-id (System/currentTimeMillis))
(defonce ^:private mount-state 42)
(defonce ^:private state-seq (atom 0))
(defonce ^:private state-order (atom {}))

(defn- make-state-seq [state]
  (or (@state-order state)
      (let [nseq (swap! state-seq inc)]
        (swap! state-order assoc state nseq)
        nseq)))

(deftype NotStartedState [state] 
  Object 
  (toString [this] 
    (str "'" state "' is not started (to start all the states call mount/start)")))

;;TODO validate stop and the fact that start and stop are fns
(defn- validate [{:keys [start stop]}]
  (when-not start 
    (throw (IllegalArgumentException. "can't start a stateful thing without a start function. (i.e. missing :start fn)")))
  {:start start :stop stop})

(defmacro defstate [state & body]
  (let [[state [c cf d df]] (macro/name-with-attributes state body)
        {:keys [start stop]} (validate {c cf d df})]
    (let [s-meta (-> {:mount-state mount-state
                      :order (make-state-seq state)
                      :start `(fn [] (~@start)) 
                      :started? false}
                     (cond-> df (assoc :stop `(fn [] (~@stop)))))]
      `(defonce ~(with-meta state (merge (meta state) s-meta))
         (NotStartedState. ~(str state))))))

(defn- up [var {:keys [ns name start started?]}]
  (when-not started?
    (info ">> starting.. " name)
    (let [s (try (start) 
                 (catch Throwable t 
                   (throw (RuntimeException. (str "could not start [" name "] due to") t))))]
      (intern ns (symbol name) s)
      (alter-meta! var assoc :started? true))))

(defn- down [var {:keys [name stop started?]}]
  (when started?
    (info "<< stopping.. " name)
    (when stop 
      (try
        (stop)
        (catch Throwable t 
          (throw (RuntimeException. (str "could not stop [" name "] due to") t)))))
    (alter-meta! var assoc :started? false)))

(defn mount-state? [var]
  (= (-> var meta :mount-state)
     mount-state))

(defn find-all-states []
  (->> (all-ns)
       (mapcat ns-interns)
       (map second)
       (filter mount-state?)))

;; TODO: narrow down by {:mount {:include-ns
;;                                {:starts-with ["app.foo" "bar.baz"]
;;                                 :nss ["app.nyse" "app.tools.datomic"] }
;;                               :exclude-ns
;;                                {:starts-with ["dont.want.this" "app.debug"]
;;                                 :nss ["dev" "app.stage"]}}}
;;
;; would come from boot/lein dev profile
(defn- bring [states fun order]
  (->> states
       (sort-by (comp :order meta) order)
       (map #(fun % (meta %)))
       doall))

(defn start [& states]
  (let [states (or states (find-all-states))]
    (bring states up <)
    :started))

(defn stop [& states]
  (let [states (or states (find-all-states))]
    (bring states down >)
    :stopped))
