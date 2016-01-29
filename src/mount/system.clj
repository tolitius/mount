(ns mount.system
  (:require [mount.core :as mount]))

(defprotocol Lifecycle
  (start [this] "starts a system")
  (stop [this] "stops a system")
  ;; (start-with [this states])
  ;; (start-without [this states])
  ;; (stop-except [this states])
  )

(defrecord MountSystem [components])

(defn- select-fun [states f]
  (into []
    (for [[name state] states]
      (when-let [fun (f state)]
        [name fun]))))

(defn- bring-system [funs]
  (into {}
    (for [[name fun] funs]
      [name (fun)])))

(defn- unvar-state [s]
  (->> s (drop 2) (apply str)))  ;; magic 2 is removing "#'" in state name

(defn unvar-names [states]
  (into {} (for [[k v] states]
             [(unvar-state k) v])))

(defn- not-started [states]
  (into {}
    (for [state (keys states)]
      [state :not-started])))

(defn- detach [sys]
  (doseq [[state {:keys [var status] :as v}] sys]
    (alter-var-root var (constantly :not-started))
    (#'mount.core/update-meta! [state :status] #{:stopped})
    (#'mount.core/update-meta! [state :var] :not-started)))

(defn- attach [sys]
  (into {}
        (for [[k {:keys [var]}] sys]
          [(unvar-state k) @var])))

(defn- spawn [sys]
  (mount/start)
  (let [spawned (attach sys)]
    (detach sys)
    spawned))

(defn new-system [meta-state]
  (let [states (-> (sort-by (comp :order val) < 
                            meta-state)
                    unvar-names)
        down (reverse (select-fun states :stop))]
    (extend-type MountSystem
      Lifecycle
      (start [states] (->MountSystem (spawn meta-state)))
      (stop [_] (bring-system down)))
    (->MountSystem (not-started states))))

(comment

(require :reload '[mount.system :as m])
(def ms @@#'mount.core/meta-state)
(def sys (new-system ms))
(def sys (m/start sys))

;; at this point the system is up and detached: i.e. "global" vars and mount do not see it
;; TODO: figure out stopping it.. i.e. need to convert stops to work with @deps
;;       ... quite possible in "cljc" :)
)
