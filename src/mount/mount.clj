(ns mount
  (:require [clojure.tools.macro :as macro]
            [clojure.tools.namespace.repl :refer [disable-reload!]]
            [clojure.tools.logging :refer [info warn debug error]]))

(disable-reload!)

;; (defonce ^:private session-id (System/currentTimeMillis))
(defonce ^:private mount-state 42)
(defonce ^:private -args (atom :no-args))                  ;; mostly for command line args and external files
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

(defn- down [var {:keys [ns name stop started?]}]
  (when started?
    (info "<< stopping.. " name)
    (when stop 
      (try
        (stop)
        (intern ns (symbol name) (NotStartedState. name))
        (catch Throwable t 
          (throw (RuntimeException. (str "could not stop [" name "] due to") t)))))
    (alter-meta! var assoc :started? false)))

;;TODO args might need more thinking
(defn args [] @-args)

(defn mount-state? [var]
  (= (-> var meta :mount-state)
     mount-state))

(defn find-all-states []
  (->> (all-ns)
       (mapcat ns-interns)
       (map second)
       (filter mount-state?)))

(defn- bring [states fun order]
  (->> states
       (sort-by (comp :order meta) order)
       (map #(fun % (meta %)))
       doall))

(defn- rollback! [state]
  (let [{:keys [origin start stop sub?]} (meta state)]
    (when origin
      (alter-meta! state assoc :origin nil
                   :start (or (:start origin) start)
                   :stop (or (:stop origin) stop)))))

(defn- unsub [state]
  (when (-> (meta state) :sub?)
    (alter-meta! state assoc :sub? nil
                             :started false)))

(defn- substitute! [state with]
  (let [{:keys [start stop] :as origin} (meta state)
        m-with (meta with)]
    (alter-meta! with assoc :sub? true :started? true) ;; pre: called by "start-with"
    (alter-meta! state assoc :origin {:start start
                                      :stop stop}
                             :start (:start m-with)
                             :stop (:stop m-with))))

(defn start [& states]
  (let [states (or (seq states) (find-all-states))]
    (bring states up <)
    :started))

(defn stop [& states]
  (let [states (or states (find-all-states))]
    (doall (map unsub states))     ;; unmark substitutions marked by "start-with"
    (bring states down >)
    (doall (map rollback! states)) ;; restore to origin from "start-with"
    :stopped))

(defn start-with-args [xs & states]
  (reset! -args xs)
  (if (first states)
    (start states)
    (start)))

(defn start-with [with]
  (let [app (find-all-states)]
    (doall
      (for [[from to] with]
        (substitute! from to)))
    (start)))

(defn start-without [& states]
  (if (first states)
    (let [app (set (find-all-states))
          without (remove (set states) app)]
      (apply start without))
    (start)))
