(ns mount.core
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

;;TODO validate the whole lifecycle
(defn- validate [{:keys [start stop suspend resume] :as lifecycle}]
  (when-not start 
    (throw (IllegalArgumentException. "can't start a stateful thing without a start function. (i.e. missing :start fn)")))
  (when (and suspend (not resume))
    (throw (IllegalArgumentException. "suspendable state should have a resume function (i.e. missing :resume fn)"))))

(defmacro defstate [state & body]
  (let [[state params] (macro/name-with-attributes state body)
        {:keys [start stop suspend resume] :as lifecycle} (apply hash-map params)]
    (validate lifecycle)
    (let [s-meta (-> {:mount-state mount-state
                      :order (make-state-seq state)
                      :start `(fn [] (~@start)) 
                      :started? false}
                     (cond-> stop (assoc :stop `(fn [] (~@stop))))
                     (cond-> suspend (assoc :suspend `(fn [] (~@suspend))))
                     (cond-> resume (assoc :resume `(fn [] (~@resume)))))]
      `(defonce ~(with-meta state (merge (meta state) s-meta))
         (NotStartedState. ~(str state))))))

(defn- up [var {:keys [ns name start started? resume suspended?]}]
  (when-not started?
    (let [s (try (if suspended?
                   (do (info ">> resuming.. " name)
                       (resume))
                   (do (info ">> starting.. " name)
                       (start)))
                 (catch Throwable t
                   (throw (RuntimeException. (str "could not start [" name "] due to") t))))]
      (intern ns (symbol name) s)
      (alter-meta! var assoc :started? true :suspended? false))))

(defn- down [var {:keys [ns name stop started? suspended?]}]
  (when (or started? suspended?)
    (info "<< stopping.. " name)
    (when stop 
      (try
        (stop)
        (catch Throwable t
          (throw (RuntimeException. (str "could not stop [" name "] due to") t)))))
    (intern ns (symbol name) (NotStartedState. name)) ;; (!) if a state does not have :stop when _should_ this might leak
    (alter-meta! var assoc :started? false :suspended? false)))

(defn- sigstop [var {:keys [ns name started? suspend resume]}]
  (when (and started? resume)        ;; can't have suspend without resume, but the reverse is possible
    (info ">> suspending.. " name)
    (when suspend                    ;; don't suspend if there is only resume function (just mark it :suspended?)
      (let [s (try (suspend)
                   (catch Throwable t
                     (throw (RuntimeException. (str "could not suspend [" name "] due to") t))))]
        (intern ns (symbol name) s)))
    (alter-meta! var assoc :started? false :suspended? true)))

(defn- sigcont [var {:keys [ns name start started? resume suspended?]}]
  (when (instance? NotStartedState var)
    (throw (RuntimeException. (str "could not resume [" name "] since it is stoppped (i.e. not suspended)"))))
  (when suspended?
    (info ">> resuming.. " name)
    (let [s (try (resume)
                 (catch Throwable t
                   (throw (RuntimeException. (str "could not resume [" name "] due to") t))))]
      (intern ns (symbol name) s)
      (alter-meta! var assoc :started? true :suspended? false))))

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

;;TODO ns based for now. need to be _state_ based
(defn- add-deps [{:keys [ns] :as state} all]
  (let [refers (ns-refers ns)
        any (set all)
        deps (filter (comp any val) refers)]
    (assoc state :deps deps)))

(defn states-with-deps []
  (let [all (find-all-states)]
    (->> (map (comp #(add-deps % all)
                    #(select-keys % [:name :order :ns :started? :suspended?])
                    meta)
              all)
         (sort-by :order))))

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

(defn stop-except [& states]
  (let [all (set (find-all-states))
        states (remove (set states) all)]
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

(defn suspend [& states]
  (let [states (or (seq states) (find-all-states))]
    (bring states sigstop <)
    :suspended))

(defn resume [& states]
  (let [states (or (seq states) (find-all-states))]
    (bring states sigcont <)
    :resumed))
