(ns app.utils.logging          ;; << change to your namespace/path 
  (:require [mount.core]
            [robert.hooke :refer [add-hook clear-hooks]]
            [clojure.string :refer [split]]
            [clojure.tools.logging :refer [info]]))

(alter-meta! *ns* assoc ::load false)

(defn- f-to-action [f]
  (let [fname (-> (str f)
                  (split #"@")
                  first)]
    (case fname
      "mount.core$up" :up
      "mount.core$down" :down
      "mount.core$sigstop" :suspend
      "mount.core$sigcont" :resume
      :noop)))

(defn whatcha-doing? [{:keys [status suspend]} action]
  (case action
    :up (if (status :suspended) ">> resuming" 
          (if-not (status :started) ">> starting"))
    :down (if (or (status :started) (status :suspended)) "<< stopping")
    :suspend (if (and (status :started) suspend) "<< suspending")
    :resume (if (status :suspended) ">> resuming")))

(defn log-status [f & args] 
  (let [{:keys [var] :as state} (second args)
        action (f-to-action f)] 
    (when-let [taking-over-the-world (whatcha-doing? state action)]
      (info (str taking-over-the-world "..  " var)))
    (apply f args)))

(defonce lifecycle-fns
  #{#'mount.core/up
    #'mount.core/down})

(defn without-logging-status []
  (doall (map #(clear-hooks %) lifecycle-fns)))

(defn with-logging-status []
  (without-logging-status)
  (doall (map #(add-hook % log-status) lifecycle-fns)))
