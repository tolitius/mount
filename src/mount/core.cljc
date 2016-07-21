(ns mount.core
  #?(:clj (:require [mount.tools.macro :refer [on-error throw-runtime] :as macro]
                    [mount.tools.logger :refer [log]]
                    [clojure.set :refer [intersection]]
                    [clojure.string :as s])
     :cljs (:require [mount.tools.macro :as macro]
                     [clojure.set :refer [intersection]]
                     [mount.tools.logger :refer [log]]))
  #?(:cljs (:require-macros [mount.core]
                            [mount.tools.macro :refer [if-clj on-error throw-runtime]])))

(defonce ^:private -args (atom {}))                        ;; mostly for command line args and external files
(defonce ^:private state-seq (atom 0))
(defonce ^:private mode (atom :clj))
(defonce ^:private meta-state (atom {}))
(defonce ^:private running (atom {}))                      ;; to clean dirty states on redefs

;; supporting tools.namespace: (disable-reload!)
#?(:clj
    (alter-meta! *ns* assoc ::load false)) ;; to exclude the dependency

(defn- make-state-seq [state]
  (or (:order (@meta-state state))
      (swap! state-seq inc)))

(deftype NotStartedState [state]
  Object
  (toString [this]
    (str "'" state "' is not started (to start all the states call mount/start)")))

;;TODO validate the whole lifecycle
(defn- validate [{:keys [start stop suspend resume] :as lifecycle}]
  (cond
    (not start) (throw-runtime "can't start a stateful thing without a start function. (i.e. missing :start fn)")
    (or suspend resume) (throw-runtime "suspend / resume lifecycle support was removed in \"0.1.10\" in favor of (mount/stop-except)")))

(defn- with-ns [ns name]
  (str "#'" ns "/" name))

(defn- pounded? [f]
  (let [pound "(fn* [] "]          ;;TODO: think of a better (i.e. typed) way to distinguish #(f params) from (fn [params] (...)))
    (.startsWith (str f) pound)))

(defn unpound [f]
  (if (pounded? f)
    (nth f 2)                      ;; magic 2 is to get the body => ["fn*" "[]" "(fn body)"]
    f))

(defn cleanup-if-dirty
  "in case a namespace is recompiled without calling (mount/stop),
   a running state instance will still be running.
   this function stops this 'lost' state instance.
   it is meant to be called by defstate before defining a new state"
  [state reason]
  (when-let [{:keys [stop] :as up} (@running state)]
    (when stop
      (log (str "<< stopping.. " state " " reason))
      (stop))
    (swap! running dissoc state)))

#?(:clj
    (defn current-state [state]
      (let [{:keys [inst var]} (@meta-state state)]
        (if (= @mode :cljc)
          @inst
          (var-get var))))

   :cljs
    (defn current-state [state]
      (-> (@meta-state state) :inst deref)))

#?(:clj
    (defn alter-state! [{:keys [var inst]} value]
      (if (= @mode :cljc)
        (reset! inst value)
        (alter-var-root var (constantly value))))

   :cljs
    (defn alter-state! [{:keys [inst]} value]
      (reset! inst value)))

(defn- update-meta! [path v]
  (swap! meta-state assoc-in path v))

(defn- record! [state-name f done]
  (let [state (f)]
    (swap! done conj state-name)
    state))

(defn- up [state {:keys [start stop status] :as current} done]
  (when-not (:started status)
    (let [s (on-error (str "could not start [" state "] due to")
                      (record! state start done))]
      (alter-state! current s)
      (swap! running assoc state {:stop stop})
      (update-meta! [state :status] #{:started}))))

(defn- down [state {:keys [stop status] :as current} done]
  (when (some status #{:started})
    (when stop
      (on-error (str "could not stop [" state "] due to")
                (record! state stop done)))
    (alter-state! current (NotStartedState. state))   ;; (!) if a state does not have :stop when _should_ this might leak
    (swap! running dissoc state)
    (update-meta! [state :status] #{:stopped})))

(deftype DerefableState [name]
  #?(:clj clojure.lang.IDeref
     :cljs IDeref)
  (#?(:clj deref
      :cljs -deref)
    [_]
    (let [{:keys [status inst] :as state} (@meta-state name)]
      (when-not (:started status)
        (up name state (atom #{})))
      @inst)))

(defn on-reload-meta [s-var]
  (or (-> s-var meta :on-reload)
      :restart))                      ;; restart by default on ns reload

;;TODO: make private after figuring out the inconsistency betwen cljs compile stages 
;;      (i.e. _sometimes_ this, if private, is not seen by expanded "defmacro" on cljs side)
(defn mount-it [s-var s-name s-meta]
  (let [with-inst (assoc s-meta :inst (atom (NotStartedState. s-name))
                                :var s-var)
        on-reload (on-reload-meta s-var)
        existing? (when-not (= :noop on-reload)
                    (cleanup-if-dirty s-name "(namespace was recompiled)"))]
    (update-meta! [s-name] with-inst)
    (when (and existing? (= :restart on-reload))
      (log (str ">> starting.. " s-name " (namespace was recompiled)"))
      (up s-name with-inst (atom #{})))))

#?(:clj
    (defmacro defstate [state & body]
      (let [[state params] (macro/name-with-attributes state body)
            {:keys [start stop] :as lifecycle} (apply hash-map params)
            state-name (with-ns *ns* state)
            order (make-state-seq state-name)]
        (validate lifecycle)
        (let [s-meta (cond-> {:order order
                              :start `(fn [] ~start)
                              :status #{:stopped}}
                       stop (assoc :stop `(fn [] ~stop)))]
          `(do
             (log (str "|| mounting... " ~state-name))
             (~'defonce ~state (DerefableState. ~state-name))
             (mount-it (~'var ~state) ~state-name ~s-meta)
             (~'var ~state))))))

#?(:clj
    (defmacro defstate! [state & {:keys [start! stop!]}]
      (let [state-name (with-ns *ns* state)]
        `(defstate ~state
           :start (~'let [~state (mount/current-state ~state-name)]
                    ~start!)
           :stop (~'let [~state (mount/current-state ~state-name)]
                   ~stop!)))))

(defn in-cljc-mode []
  (reset! mode :cljc))

(defn in-clj-mode []
  (reset! mode :clj))

;;TODO args might need more thinking
(defn args [] @-args)

(defn- find-all-states []
  (keys @meta-state))

#?(:clj
    (defn- var-to-str [v]
      (str v)))

#?(:cljs
    (defn var-to-str [v]
      (if (instance? cljs.core.Var v)
        (let [{:keys [ns name]} (meta v)]
          (with-ns ns name))
        v)))

(defn running-states []
  (keys @running))

(defn- unvar-state [s]
  (->> s (drop 2) (apply str)))  ;; magic 2 is removing "#'" in state name

#?(:clj
    (defn- was-removed?
      "checks if a state was removed from a namespace"
      [state]
      (-> state unvar-state symbol resolve not)))

#?(:clj
    (defn cleanup-deleted [state]
      (when (was-removed? state)
        (cleanup-if-dirty state "(it was deleted)")
        (swap! meta-state dissoc state))))

(defn- bring [states fun order]
  (let [done (atom [])]
    (as-> states $
          (map var-to-str $)
          #?(:clj                          ;; needs more thking in cljs, since based on sym resolve
              (remove cleanup-deleted $))
          (select-keys @meta-state $)
          (sort-by (comp :order val) order $)
          (doseq [[k v] $] (fun k v done)))
    @done))

(defn- merge-lifecycles
  "merges with overriding _certain_ non existing keys.
   i.e. :stop is in a 'state', but not in a 'substitute': it should be overriden with nil
        however other keys of 'state' (such as :ns,:name,:order) should not be overriden"
  ([state sub]
    (merge-lifecycles state nil sub))
  ([state origin {:keys [start stop status]}]
    (assoc state :origin origin
                 :status status
                 :start start :stop stop)))

(defn- rollback! [state]
  (let [{:keys [origin] :as sub} (@meta-state state)]
    (when origin
      (update-meta! [state] (merge-lifecycles sub origin)))))

(defn- substitute! [state with mode]
  (let [lifecycle-fns #(select-keys % [:start :stop :status])
        origin (@meta-state state)
        sub (if (= :value mode)
              {:start (fn [] with) :status :stopped}
              (@meta-state with))]
    (when (= :state mode)
      (update-meta! [with :sub?] true))
    (update-meta! [state] (merge-lifecycles origin (lifecycle-fns origin) sub))))

(defn- unsub [state]
  (when (-> (@meta-state state) :sub?)
    (update-meta! [state :sub?] nil)))

(defn- all-without-subs []
  (remove (comp :sub? @meta-state) (find-all-states)))

(defn start [& states]
  (if (-> states first coll?)
    (apply start (first states))
    (let [states (or (seq states) (all-without-subs))]
      {:started (bring states up <)})))

(defn stop [& states]
  (let [states (or states (find-all-states))
        _ (dorun (map unsub states))     ;; unmark substitutions marked by "start-with"
        stopped (bring states down >)]
    (dorun (map rollback! states))       ;; restore to origin from "start-with"
    {:stopped stopped}))

;; composable set of states

(defn- mapset [f xs]
  (-> (map f xs)
      set))

(defn only
  ([states]
   (only (find-all-states) states))
  ([states these]
   (intersection (mapset var-to-str these)
                 (mapset var-to-str states))))

(defn with-args 
  ([args]
   (with-args (find-all-states) args))
  ([states args]
    (reset! -args args)  ;; TODO localize
    states))

(defn except 
  ([states]
   (except (find-all-states) states))
  ([states these]
   (remove (mapset var-to-str these)
           (mapset var-to-str states))))

(defn swap
  ([with]
   (swap (find-all-states) with))
  ([states with]
   (doseq [[from to] with]
     (substitute! (var-to-str from)
                  to :value))
   states))

(defn swap-states
  ([with]
   (swap-states (find-all-states) with))
  ([states with]
   (doseq [[from to] with]
     (substitute! (var-to-str from)
                  (var-to-str to) :state))
   states))

;; explicit, not composable (subject to depreciate?)

(defn stop-except [& states]
  (let [all (set (find-all-states))
        states (map var-to-str states)
        states (remove (set states) all)]
    (apply stop states)))

(defn start-with-args [xs & states]
  (reset! -args xs)
  (if (first states)
    (apply start states)
    (start)))

(defn start-with [with]
  (doseq [[from to] with]
    (substitute! (var-to-str from)
                 to :value))
  (start))

(defn start-with-states [with]
  (doseq [[from to] with]
    (substitute! (var-to-str from)
                 (var-to-str to) :state))
  (start))

(defn start-without [& states]
  (if (first states)
    (let [app (set (all-without-subs))
          states (map var-to-str states)
          without (remove (set states) app)]
      (apply start without))
    (start)))
