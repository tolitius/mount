(ns mount.test.start-with-states
  (:require
    #?@(:cljs [[cljs.test :as t :refer-macros [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer-macros [defstate]]
               [tapp.websockets :refer [system-a]]
               [tapp.conf :refer [config]]
               [tapp.audit-log :refer [log]]]
        :clj  [[clojure.test :as t :refer [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer [defstate]]
               [clojure.tools.nrepl.server :refer [start-server stop-server]]
               [tapp.conf :refer [config]]
               [tapp.nyse :refer [conn]]
               [tapp.example :refer [nrepl]]])
   [mount.test.helper :refer [dval helper]]))

#?(:clj (alter-meta! *ns* assoc ::load false))

(defstate test-conn :start 42
                    :stop (constantly 0))

(defstate test-nrepl :start [])

(def swap-conn {:start (fn [] 42)
                :stop #(println "stopping test-conn-state")})
#?(:clj
  (def swap-nrepl {:start #(start-server :bind "localhost" :port 3442)
                   :stop #(stop-server @nrepl)})
  :cljs
  (def swap-nrepl {:start (fn [] :nrepl)
                   :stop (fn [] :stopped-nrepl)}))

#?(:cljs
  (deftest start-with-states

    (testing "should start with substitutes"
      (let [_ (mount/start-with-states {#'tapp.websockets/system-a swap-conn
                                        #'mount.test.helper/helper swap-nrepl})]
        (is (map? (dval config)))
        (is (= (:nrepl (dval helper))))
        (is (= (dval system-a) 42))
        (is (instance? datascript.db/DB @(dval log)))
        (mount/stop)))

    #_(testing "should not start the substitute itself"                         ;; was true when subbing with exsiting states
      (let [_ (mount/start-with-states {#'tapp.websockets/system-a swap-conn})]
        (is (instance? mount.core.NotStartedState (dval test-conn)))
        (is (= 42 (dval system-a)))
        (mount/stop)))

    (testing "should start normally after start-with-states"
      (let [_ (mount/start)]
        (is (map? (dval config)))
        (is (instance? datascript.db/DB @(dval log)))
        (is (instance? js/WebSocket (dval system-a)))
        (is (= 42 (dval test-conn)))
        (is (vector? (dval test-nrepl)))
        (is (= :started (dval helper)))
        (mount/stop)))

    (testing "should start-without normally after start-with-states"
      (let [_ (mount/start-without #'mount.test.start-with-states/test-conn
                                   #'mount.test.start-with-states/test-nrepl)]
        (is (map? (dval config)))
        (is (instance? datascript.db/DB @(dval log)))
        (is (instance? js/WebSocket (dval system-a)))
        (is (= :started (dval helper)))
        (is (instance? mount.core.NotStartedState (dval test-conn)))
        (is (instance? mount.core.NotStartedState (dval test-nrepl)))
        (mount/stop)))))

#?(:clj
  (deftest start-with-states

    (testing "should start with substitutes"
      (let [_ (mount/start-with-states {#'tapp.nyse/conn swap-conn
                                        #'tapp.example/nrepl swap-nrepl})]
        (is (map? (dval config)))
        (is (instance? clojure.tools.nrepl.server.Server (dval nrepl)))
        (is (= (dval conn) 42))
        (mount/stop)))

    #_(testing "should not start the substitute itself"                         ;; was true when subbing with exsiting states
      (let [_ (mount/start-with-states {#'tapp.nyse/conn swap-conn})]
        (is (instance? mount.core.NotStartedState (dval test-conn)))
        (is (= (dval conn) 42))
        (mount/stop)))

    (testing "should start normally after start-with-states"
      (let [_ (mount/start)]
        (is (map? (dval config)))
        (is (instance? clojure.tools.nrepl.server.Server (dval nrepl)))
        (is (instance? datomic.peer.LocalConnection (dval conn)))
        (is (= (dval test-conn) 42))
        (is (vector? (dval test-nrepl)))
        (mount/stop)))

    (testing "should start-without normally after start-with-states"
      (let [_ (mount/start-without #'mount.test.start-with-states/test-conn
                                   #'mount.test.start-with-states/test-nrepl)]
        (is (map? (dval config)))
        (is (instance? clojure.tools.nrepl.server.Server (dval nrepl)))
        (is (instance? datomic.peer.LocalConnection (dval conn)))
        (is (instance? mount.core.NotStartedState (dval test-conn)))
        (is (instance? mount.core.NotStartedState (dval test-nrepl)))
        (mount/stop)))))
