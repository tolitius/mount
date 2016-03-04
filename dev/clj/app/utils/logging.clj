(ns app.utils.logging          ;; << change to your namespace/path 
  (:require [mount.core]
            [robert.hooke :refer [add-hook clear-hooks]]
            [clojure.string :refer [split]]
            [clojure.tools.logging :refer [info]]))

(alter-meta! *ns* assoc ::load false)

(defn- f-to-action [f {:keys [status]}]
  (let [fname (-> (str f)
                  (split #"@")
                  first)]
    (case fname
      "mount.core$up" (when-not (:started status) :up)
      "mount.core$down" (when-not (:stopped status) :down)
      :noop)))

(defn whatcha-doing? [action]
  (case action
    :up ">> starting"
    :down "<< stopping"
    false))

(defn log-status [f & args] 
  (let [[state-name state] args
        action (f-to-action f state)] 
    (when-let [taking-over-the-world (whatcha-doing? action)]
      (info (str taking-over-the-world ".. " state-name)))
    (apply f args)))

(defonce lifecycle-fns
  #{#'mount.core/up
    #'mount.core/down})

(defn without-logging-status []
  (doall (map #(clear-hooks %) lifecycle-fns)))


;; this is the one to use:

(defn with-logging-status []
  (without-logging-status)
  (doall (map #(add-hook % log-status) lifecycle-fns)))
