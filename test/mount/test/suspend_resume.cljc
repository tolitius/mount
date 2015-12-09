(ns mount.test.suspend-resume
  (:require
    #?@(:cljs [[cljs.test :as t :refer-macros [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer-macros [defstate]]
               [app.websockets :refer [system-a]]
               [app.conf :refer [config]]
               [app.audit-log :refer [log]]]
        :clj  [[clojure.test :as t :refer [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer [defstate]]
               [app.conf :refer [config]]
               [app.nyse :refer [conn]]
               [app.example :refer [nrepl]]])
   [mount.test.helper :refer [dval]]))

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

(defstate randomizer :start (rand-int 42))

#?(:clj
  (deftest suspendable-lifecycle

    (testing "should suspend _only suspendable_ states that are currently started"
      (let [_ (mount/start)
            _ (mount/suspend)]
        (is (map? (dval config)))
        (is (instance? clojure.tools.nrepl.server.Server (dval nrepl)))
        (is (instance? datomic.peer.LocalConnection (dval conn)))
        (is (= (dval web-server) :w-suspended))
        (mount/stop)))
    
    (testing "should resume _only suspendable_ states that are currently suspended"
      (let [_ (mount/start)
            _ (mount/stop #'app.example/nrepl)
            _ (mount/suspend)
            _ (mount/resume)]
        (is (map? (dval config)))
        (is (instance? mount.core.NotStartedState (dval nrepl)))
        (is (instance? datomic.peer.LocalConnection (dval conn)))
        (is (= (dval web-server) :w-resumed))
        (mount/stop)))

    (testing "should start all the states, except the ones that are currently suspended, should resume them instead"
      (let [_ (mount/start)
            _ (mount/suspend)
            _ (mount/start)]
        (is (map? (dval config)))
        (is (instance? clojure.tools.nrepl.server.Server (dval nrepl)))
        (is (instance? datomic.peer.LocalConnection (dval conn)))
        (is (= (dval web-server) :w-resumed))
        (mount/stop)))

    (testing "should stop all: started and suspended"
      (let [_ (mount/start)
            _ (mount/suspend)
            _ (mount/stop)]
        (is (instance? mount.core.NotStartedState (dval config)))
        (is (instance? mount.core.NotStartedState (dval nrepl)))
        (is (instance? mount.core.NotStartedState (dval conn)))
        (is (instance? mount.core.NotStartedState (dval web-server)))))))


#?(:clj 
  (deftest suspendable-start-with

    (testing "when replacing a non suspendable state with a suspendable one,
              the later should be able to suspend/resume,
              the original should not be suspendable after resume and preserve its lifecycle fns after rollback/stop"
      (let [_ (mount/start-with {#'app.example/nrepl #'mount.test.suspend-resume/web-server})
            _ (mount/suspend)]
        (is (= (dval nrepl) :w-suspended))
        (is (instance? mount.core.NotStartedState (dval web-server)))
        (mount/stop)
        (mount/start)
        (mount/suspend)
        (is (instance? clojure.tools.nrepl.server.Server (dval nrepl)))
        (is (= (dval web-server) :w-suspended))
        (mount/stop)))

    ;; this is a messy use case, but can still happen especially at REPL time
    ;; it also messy, because usually :stop function refers the _original_ state by name (i.e. #(disconnect conn))
    ;;    (unchanged/not substituted in its lexical scope), and original state won't be started
    (testing "when replacing a suspendable state with a non suspendable one,
              the later should not be suspendable,
              the original should still be suspendable and preserve its lifecycle fns after the rollback/stop"
      (let [_ (mount/start-with {#'mount.test.suspend-resume/web-server #'mount.test.suspend-resume/randomizer})
            _ (mount/suspend)]
        (is (integer? (dval web-server)))
        (is (instance? mount.core.NotStartedState (dval randomizer)))
        (mount/stop)
        (mount/start)
        (mount/suspend)
        (is (integer? (dval randomizer)))
        (is (= (dval web-server) :w-suspended))
        (mount/stop)))

    ;; this is a messy use case, but can still happen especially at REPL time
    (testing "when replacing a suspended state with a non suspendable started one,
              the later should not be suspendable,
              the original should still be suspended and preserve its lifecycle fns after the rollback/stop"
      (let [_ (mount/start)
            _ (mount/suspend) 
            _ (mount/start-with {#'mount.test.suspend-resume/web-server #'app.nyse/conn})  ;; TODO: good to WARN on started states during "start-with"
            _ (mount/suspend)]
        (is (instance? datomic.peer.LocalConnection (dval conn)))
        (is (= (dval web-server) :w-suspended)) ;; since the "conn" does not have a resume method, so web-server was not started
        (mount/stop)
        (mount/start)
        (mount/suspend)
        (is (instance? datomic.peer.LocalConnection (dval conn)))
        (is (= (dval web-server) :w-suspended))
        (mount/stop)))

    ;; this is a messy use case, but can still happen especially at REPL time
    (testing "when replacing a suspended state with a suspendable one,
              the later should be suspendable,
              the original should still be suspended and preserve its lifecycle fns after the rollback/stop"
      (let [_ (mount/start)
            _ (mount/suspend) 
            _ (mount/start-with {#'mount.test.suspend-resume/web-server 
                                 #'mount.test.suspend-resume/q-listener})]  ;; TODO: good to WARN on started states during "start-with"
        (is (= (dval q-listener) :q-suspended))
        (is (= (dval web-server) :q-resumed))
        (mount/suspend)
        (is (= (dval q-listener) :q-suspended))
        (is (= (dval web-server) :q-suspended))
        (mount/stop)
        (is (instance? mount.core.NotStartedState (dval web-server)))
        (is (instance? mount.core.NotStartedState (dval q-listener)))
        (mount/start)
        (mount/suspend)
        (is (= (dval q-listener) :q-suspended))
        (is (= (dval web-server) :w-suspended))
        (mount/stop)))))
