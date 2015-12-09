(ns mount.test.start-with
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

(defstate test-conn :start 42
                    :stop (constantly 0))

(defstate test-nrepl :start [])

#?(:clj
  (deftest start-with

    (testing "should start with substitutes"
      (let [_ (mount/start-with {#'app.nyse/conn #'mount.test.start-with/test-conn
                                 #'app.example/nrepl #'mount.test.start-with/test-nrepl})]
        (is (map? (dval config)))
        (is (vector? (dval nrepl)))
        (is (= (dval conn) 42))
        (mount/stop)))
    
    (testing "should not start the substitute itself"
      (let [_ (mount/start-with {#'app.nyse/conn #'mount.test.start-with/test-conn})]
        (is (instance? mount.core.NotStartedState (dval test-conn)))
        (is (= (dval conn) 42))
        (mount/stop)))

    (testing "should start normally after start-with"
      (let [_ (mount/start)]
        (is (map? (dval config)))
        (is (instance? clojure.tools.nrepl.server.Server (dval nrepl)))
        (is (instance? datomic.peer.LocalConnection (dval conn)))
        (is (= (dval test-conn )42))
        (is (vector? (dval test-nrepl)))
        (mount/stop)))

    (testing "should start-without normally after start-with"
      (let [_ (mount/start-without #'mount.test.start-with/test-conn
                                   #'mount.test.start-with/test-nrepl)]
        (is (map? (dval config)))
        (is (instance? clojure.tools.nrepl.server.Server (dval nrepl)))
        (is (instance? datomic.peer.LocalConnection (dval conn)))
        (is (instance? mount.core.NotStartedState (dval test-conn)))
        (is (instance? mount.core.NotStartedState (dval test-nrepl)))
        (mount/stop)))))
