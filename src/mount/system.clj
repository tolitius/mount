(ns mount.system)

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

(defn new-system [meta-state]
  (let [states (-> (sort-by (comp :order val) < 
                            meta-state)
                   unvar-names)
        up (select-fun states :start)
        down (reverse (select-fun states :stop))]
    (extend-type MountSystem
      Lifecycle
      (start [_] (bring-system up))   ;; these two will have two lift inter var deps
      (stop [_] (bring-system down))) ;; 
    (->MountSystem (not-started states))))
