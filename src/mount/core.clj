(ns mount.core
  (:require [clojure.tools.macro :as macro]))

(defonce ^:private mount-state 42)
(defonce ^:private -args (atom :no-args))                  ;; mostly for command line args and external files
(defonce ^:private state-seq (atom 0))
(defonce ^:private state-order (atom {}))

;; supporting tools.namespace: (disable-reload!)
(alter-meta! *ns* assoc ::load false) ;; to exclude the dependency

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
  (cond 
    (not start) (throw 
                  (IllegalArgumentException. "can't start a stateful thing without a start function. (i.e. missing :start fn)"))
    (and suspend (not resume)) (throw 
                                 (IllegalArgumentException. "suspendable state should have a resume function (i.e. missing :resume fn)"))))

(defmacro defstate [state & body]
  (let [[state params] (macro/name-with-attributes state body)
        {:keys [start stop suspend resume] :as lifecycle} (apply hash-map params)]
    (validate lifecycle)
    (let [s-meta (cond-> {:mount-state mount-state
                          :order (make-state-seq state)
                          :start `(fn [] ~start) 
                          :status #{:stopped}}
                   stop (assoc :stop `(fn [] ~stop))
                   suspend (assoc :suspend `(fn [] ~suspend))
                   resume (assoc :resume `(fn [] ~resume)))]
      `(defonce ~(with-meta state (merge (meta state) s-meta))
         (NotStartedState. ~(str state))))))

(defn- record! [{:keys [ns name]} f done]
  (let [state (trampoline f)]
    (swap! done conj (ns-resolve ns name))
    state))

(defn- up [var {:keys [ns name start resume status] :as state} done]
  (when-not (:started status)
    (let [s (try (if (:suspended status)
                   (record! state resume done)
                   (record! state start done))
                 (catch Throwable t
                   (throw (RuntimeException. (str "could not start [" name "] due to") t))))]
      (intern ns (symbol name) s)
      (alter-meta! var assoc :status #{:started}))))

(defn- down [var {:keys [ns name stop status] :as state} done]
  (when (some status #{:started :suspended})
    (when stop 
      (try
        (record! state stop done)
        (catch Throwable t
          (throw (RuntimeException. (str "could not stop [" name "] due to") t)))))
    (intern ns (symbol name) (NotStartedState. name)) ;; (!) if a state does not have :stop when _should_ this might leak
    (alter-meta! var assoc :status #{:stopped})))

(defn- sigstop [var {:keys [ns name suspend resume status] :as state} done]
  (when (and (:started status) resume)           ;; can't have suspend without resume, but the reverse is possible
    (when suspend                                ;; don't suspend if there is only resume function (just mark it :suspended?)
      (let [s (try (record! state suspend done)
                   (catch Throwable t
                     (throw (RuntimeException. (str "could not suspend [" name "] due to") t))))]
        (intern ns (symbol name) s)))
    (alter-meta! var assoc :status #{:suspended})))

(defn- sigcont [var {:keys [ns name start resume status] :as state} done]
  (when (instance? NotStartedState var)
    (throw (RuntimeException. (str "could not resume [" name "] since it is stoppped (i.e. not suspended)"))))
  (when (:suspended status)
    (let [s (try (record! state resume done)
                 (catch Throwable t
                   (throw (RuntimeException. (str "could not resume [" name "] due to") t))))]
      (intern ns (symbol name) s)
      (alter-meta! var assoc :status #{:started}))))

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
                    #(select-keys % [:name :order :ns :status])
                    meta)
              all)
         (sort-by :order))))

(defn- bring [states fun order]
  (let [done (atom [])]
    (->> states
         (sort-by (comp :order meta) order)
         (map #(fun % (meta %) done))
         dorun)
    @done))

(defn- merge-lifecycles
  "merges with overriding _certain_ non existing keys. 
   i.e. :suspend is in a 'state', but not in a 'substitute': it should be overriden with nil
        however other keys of 'state' (such as :ns,:name,:order) should not be overriden"
  ([state sub]
    (merge-lifecycles state nil sub))
  ([state origin {:keys [start stop suspend resume status]}]
    (assoc state :origin origin 
                 :status status
                 :start start :stop stop :suspend suspend :resume resume)))

(defn- rollback! [state]
  (let [{:keys [origin]} (meta state)]
    (when origin
      (alter-meta! state #(merge-lifecycles % origin)))))

(defn- substitute! [state with]
  (let [lifecycle-fns #(select-keys % [:start :stop :suspend :resume :status])
        origin (meta state)
        sub (meta with)]
    (alter-meta! with assoc :sub? true)
    (alter-meta! state #(merge-lifecycles % (lifecycle-fns origin) sub))))

(defn- unsub [state]
  (when (-> (meta state) :sub?)
    (alter-meta! state dissoc :sub?)))

(defn- all-without-subs []
  (remove (comp :sub? meta) (find-all-states)))

(defn start [& states]
  (let [states (or (seq states) (all-without-subs))]
    {:started (bring states up <)}))

(defn stop [& states]
  (let [states (or states (find-all-states))
        _ (dorun (map unsub states))     ;; unmark substitutions marked by "start-with"
        stopped (bring states down >)]
    (dorun (map rollback! states))       ;; restore to origin from "start-with"
    {:stopped stopped}))

(defn stop-except [& states]
  (let [all (set (find-all-states))
        states (remove (set states) all)]
    (apply stop states)))

(defn start-with-args [xs & states]
  (reset! -args xs)
  (if (first states)
    (start states)
    (start)))

(defn start-with [with]
  (doseq [[from to] with]
    (substitute! from to))
  (start))

(defn start-without [& states]
  (if (first states)
    (let [app (set (all-without-subs))
          without (remove (set states) app)]
      (apply start without))
    (start)))

(defn suspend [& states]
  (let [states (or (seq states) (all-without-subs))]
    {:suspended (bring states sigstop <)}))

(defn resume [& states]
  (let [states (or (seq states) (all-without-subs))]
    {:resumed (bring states sigcont <)}))
