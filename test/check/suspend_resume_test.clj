(ns check.suspend-resume-test
  (:require [mount.core :as mount :refer [defstate]]
            [app.config :refer [app-config]]
            [app.nyse :refer [conn]]
            [app :refer [nrepl]]
            [clojure.test :refer :all]))

(defn koncat [k s]
  (-> (name k)
      (str "-" (name s))
      keyword))

(defn start [s] (koncat s :started))
(defn stop [s] (koncat s :stopped))
(defn suspend [s] (koncat s :suspended))
(defn resume [s] (koncat s :resumed))

(defstate web-server :start (start :w)
                     :stop (stop :w)
                     :suspend (suspend :w)
                     :resume (resume :w))

(defstate q-listener :start (start :q)
                     :stop (stop :q)
                     :suspend (suspend :q)
                     :resume (resume :q))

(deftest suspendable

  ;; lifecycle

  (testing "should suspend _only suspendable_ states that are currently started"
    (let [_ (mount/start)
          _ (mount/suspend)]
      (is (map? app-config))
      (is (instance? clojure.tools.nrepl.server.Server nrepl))
      (is (instance? datomic.peer.LocalConnection conn))
      (is (= web-server :w-suspended))
      (mount/stop)))
  
  (testing "should resume _only suspendable_ states that are currently suspended"
    (let [_ (mount/start)
          _ (mount/stop #'app/nrepl)
          _ (mount/suspend)
          _ (mount/resume)]
      (is (map? app-config))
      (is (instance? mount.core.NotStartedState nrepl))
      (is (instance? datomic.peer.LocalConnection conn))
      (is (= web-server :w-resumed))
      (mount/stop)))

  (testing "should start all the states, except the ones that are currently suspended, should resume them instead"
    (let [_ (mount/start)
          _ (mount/suspend)
          _ (mount/start)]
      (is (map? app-config))
      (is (instance? clojure.tools.nrepl.server.Server nrepl))
      (is (instance? datomic.peer.LocalConnection conn))
      (is (= web-server :w-resumed))
      (mount/stop)))

  (testing "should stop all: started and suspended"
    (let [_ (mount/start)
          _ (mount/suspend)
          _ (mount/stop)]
      (is (instance? mount.core.NotStartedState app-config))
      (is (instance? mount.core.NotStartedState nrepl))
      (is (instance? mount.core.NotStartedState conn))
      (is (instance? mount.core.NotStartedState web-server))))

  ;; start-with

  (testing "when replacing a non suspendable state with a suspendable one,
            the later should be able to suspend/resume,
            the original should not be suspendable after resume and preserve its lifecycle fns after rollback/stop"
    (let [_ (mount/start-with {#'app/nrepl #'check.suspend-resume-test/web-server})
          _ (mount/suspend)]
      (is (= nrepl :w-suspended))
      (is (instance? mount.core.NotStartedState web-server))
      (mount/stop)
      (mount/start)
      (mount/suspend)
      (is (instance? clojure.tools.nrepl.server.Server nrepl))
      (is (= web-server :w-suspended))
      (mount/stop)))

  ;; this is a messy use case, but can still happen especially at REPL time
  (testing "when replacing a suspended state with a non suspendable one,
            the later should not be suspendable,
            the original should still be suspended and preserve its lifecycle fns after the rollback/stop"
    (let [_ (mount/start)
          _ (mount/suspend) 
          _ (mount/start-with {#'check.suspend-resume-test/web-server #'app.nyse/conn})  ;; TODO: good to WARN on started states during "start-with"
          _ (mount/suspend)]
      (is (instance? datomic.peer.LocalConnection conn))
      (is (instance? datomic.peer.LocalConnection web-server))
      (mount/stop)
      (mount/start)
      (mount/suspend)
      (is (instance? datomic.peer.LocalConnection conn))
      (is (= web-server :w-suspended))
      (mount/stop)))

  ;; this is a messy use case, but can still happen especially at REPL time
  (testing "when replacing a suspended state with a suspendable one,
            the later should be suspendable,
            the original should still be suspended and preserve its lifecycle fns after the rollback/stop"
    (let [_ (mount/start)
          _ (mount/suspend) 
          _ (mount/start-with {#'check.suspend-resume-test/web-server #'check.suspend-resume-test/q-listener})]  ;; TODO: good to WARN on started states during "start-with"
      (is (= q-listener :q-suspended))
      (is (= web-server :q-resumed))
      (mount/suspend)
      (is (= q-listener :q-suspended))
      (is (= web-server :q-suspended))
      (mount/stop)
      (is (instance? mount.core.NotStartedState web-server))
      (is (instance? mount.core.NotStartedState q-listener))
      (mount/start)
      (mount/suspend)
      (is (= q-listener :q-suspended))
      (is (= web-server :w-suspended))
      (mount/stop))))
