(ns mount.test.stop-except
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
   [mount.test.helper :refer [dval helper]]))

#?(:cljs
  (deftest stop-except

    (testing "should stop all except nrepl"
      (let [_ (mount/start)
            _ (mount/stop-except #'app.audit-log/log #'mount.test.helper/helper)]
        (is (= :started (dval helper)))
        (is (instance? datascript.db/DB @(dval log)))
        (is (instance? mount.core.NotStartedState (dval config)))
        (is (instance? mount.core.NotStartedState (dval system-a)))
        (mount/stop)))
    
    (testing "should start normally after stop-except"
      (let [_ (mount/start)]
        (is (map? (dval config)))
        (is (instance? js/WebSocket (dval system-a)))
        (is (instance? datascript.db/DB @(dval log)))
        (mount/stop)))

    (testing "should stop all normally after stop-except"
      (let [_ (mount/start)
            _ (mount/stop-except #'app.audit-log/log #'mount.test.helper/helper)
            _ (mount/stop)]
        (is (instance? mount.core.NotStartedState (dval config)))
        (is (instance? mount.core.NotStartedState (dval log)))
        (is (instance? mount.core.NotStartedState (dval system-a)))))))

#?(:clj
  (deftest stop-except

    (testing "should stop all except nrepl"
      (let [_ (mount/start)
            _ (mount/stop-except #'app.nyse/conn #'app.conf/config)]
        (is (map? (dval config)))
        (is (instance? datomic.peer.LocalConnection (dval conn)))
        (is (instance? mount.core.NotStartedState (dval nrepl)))
        (mount/stop)))
    
    (testing "should start normally after stop-except"
      (let [_ (mount/start)]
        (is (map? (dval config)))
        (is (instance? clojure.tools.nrepl.server.Server (dval nrepl)))
        (is (instance? datomic.peer.LocalConnection (dval conn)))
        (mount/stop)))

    (testing "should stop all normally after stop-except"
      (let [_ (mount/start)
            _ (mount/stop-except #'app.nyse/conn #'app.conf/config)
            _ (mount/stop)]
        (is (instance? mount.core.NotStartedState (dval config)))
        (is (instance? mount.core.NotStartedState (dval conn)))
        (is (instance? mount.core.NotStartedState (dval nrepl)))))))
