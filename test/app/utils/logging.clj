(ns app.utils.logging
  (:require [robert.hooke :refer [add-hook clear-hooks]]
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

(defn whatcha-doing? [{:keys [started? suspended? suspend]} action]
  (case action
    :up (if suspended? "resuming" 
          (if-not started? "starting"))
    :down (if (or started? suspended?) "stopping")
    :suspend (if (and started? suspend) "suspending")
    :resume (if suspended? "resuming")))

(defn log-status [f & args] 
  (let [{:keys [ns name] :as state} (second args)
        action (f-to-action f)] 
    (when-let [taking-over-the-world (whatcha-doing? state action)]
      (info (str ">> " taking-over-the-world "..  " (ns-resolve ns name))))
    (apply f args)))

(defonce lifecycle-fns
  #{#'mount.core/up
    #'mount.core/down
    #'mount.core/sigstop
    #'mount.core/sigcont})

(defn with-logging-status []
  (doall (map #(add-hook % log-status) lifecycle-fns)))

(defn without-logging-status []
  (doall (map #(clear-hooks %) lifecycle-fns)))


;; here is just to illustrate lificycle of states in REPL
;; if needed in reality will be called in "-main" or another entry point

(without-logging-status)
(with-logging-status)
