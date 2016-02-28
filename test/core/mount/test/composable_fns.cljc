(ns mount.test.composable-fns
  (:require
    #?@(:cljs [[cljs.test :as t :refer-macros [is are deftest testing use-fixtures]]
               [clojure.set :refer [intersection]]
               [mount.core :refer [only except swap swap-states with-args] :as mount :refer-macros [defstate]]
               [tapp.websockets :refer [system-a]]
               [tapp.conf :refer [config]]
               [tapp.audit-log :refer [log]]]
        :clj  [[clojure.test :as t :refer [is are deftest testing use-fixtures]]
               [clojure.set :refer [intersection]]
               [mount.core :as mount :refer [defstate only except swap swap-states with-args]]
               [tapp.conf :refer [config]]
               [tapp.nyse :refer [conn]]
               [tapp.example :refer [nrepl]]])
   [mount.test.helper :refer [dval helper]]))

#?(:clj (alter-meta! *ns* assoc ::load false))

(defstate test-conn :start 42
                    :stop (constantly 0))

(defstate test-nrepl :start [])

#?(:clj
  (deftest only-states

    (testing "only should only return given states. 
              if source set of states is not provided, it should use all the states to select from"
      (is (= #{"#'mount.test.composable-fns/test-conn" "#'tapp.example/nrepl" "#'tapp.nyse/conn"} 
             (only #{"#'is.not/here" #'mount.test.composable-fns/test-conn #'tapp.example/nrepl #'tapp.nyse/conn}))))

    (testing "only should only return given states"
      (is (= #{"#'mount.test.composable-fns/test-conn" "#'tapp.example/nrepl"} 
             (only [#'mount.test.composable-fns/test-conn #'tapp.example/nrepl #'tapp.nyse/conn] 
                   #{"#'is.not/here" #'mount.test.composable-fns/test-conn #'tapp.example/nrepl}))))))

#?(:clj
  (deftest except-states

    (testing "except should exclude given states.
              if source set of states is not provided, it should use all the states to exclude from"
      (let [states (except #{"#'is.not/here" #'tapp.example/nrepl #'tapp.nyse/conn})]
        (is (coll? states))
        (is (pos? (count states)))
        (is (zero? (count (intersection (set states)
                                        #{"#'tapp.example/nrepl" "#'tapp.nyse/conn" "#'is.not/here"}))))))

    (testing "except should exclude given states"
      (is (= #{"#'tapp.conf/config" "#'mount.test.composable-fns/test-conn"}
             (set (except #{#'tapp.example/nrepl #'tapp.conf/config #'mount.test.composable-fns/test-conn}
                          #{"#'is.not/here" #'tapp.example/nrepl #'tapp.nyse/conn})))))))

#?(:clj
  (deftest states-with-args

    (testing "with-args should set args and return all states if none provided"
      (let [states (with-args {:a 42})]
        (is (= {:a 42} (mount/args)))
        (is (= states (#'mount.core/find-all-states)))))

    (testing "with-args should set args and thread states if provided"
      (let [t-states #{"#'is.not/here" #'mount.test.composable-fns/test-conn #'tapp.example/nrepl #'tapp.nyse/conn}
            states (with-args t-states {:a 42})]
        (is (= {:a 42} (mount/args)))
        (is (= states t-states))))))

#?(:clj
  (deftest swap-states-with-values

    (testing "swap should swap states with values and return all states if none is given"
      (let [states (swap {#'tapp.nyse/conn "conn-sub"
                          #'tapp.example/nrepl :nrepl-sub})]
        (is (= states (#'mount.core/find-all-states)))
        (mount/start)
        (is (map? (dval config)))
        (is (= :nrepl-sub (dval nrepl)))
        (is (= "conn-sub" (dval conn)))
        (mount/stop)))

    (testing "swap should swap states with values and return only states that it is given"
      (let [t-states #{"#'is.not/here" #'mount.test.composable-fns/test-conn #'tapp.example/nrepl #'tapp.nyse/conn}
            states (swap t-states {#'tapp.nyse/conn "conn-sub"
                                   #'tapp.example/nrepl :nrepl-sub})]
        (is (= states t-states))
        (mount/start)
        (is (map? (dval config)))
        (is (= :nrepl-sub (dval nrepl)))
        (is (= "conn-sub" (dval conn)))
        (is (= 42 (dval test-conn)))
        (mount/stop)))))

#?(:clj
  (deftest swap-states-with-states

    (testing "swap-states should swap states with states and return all mount states if none is given"
      (let [states (swap-states {#'tapp.nyse/conn #'mount.test.composable-fns/test-conn
                                 #'tapp.example/nrepl #'mount.test.composable-fns/test-nrepl})]
        (is (= states (#'mount.core/find-all-states)))
        (mount/start)
        (is (map? (dval config)))
        (is (vector? (dval nrepl)))
        (is (= 42 (dval conn)))
        (mount/stop)))

    (testing "swap-states should swap states with states and return only states that it is given"
      (let [t-states #{"#'is.not/here" #'mount.test.composable-fns/test-conn #'tapp.nyse/conn}
            states (swap-states t-states {#'tapp.nyse/conn #'mount.test.composable-fns/test-conn
                                          #'tapp.example/nrepl #'mount.test.composable-fns/test-nrepl})]
        (is (= states t-states))
        (apply mount/start states)
        (is (instance? mount.core.NotStartedState (dval config)))
        (is (instance? mount.core.NotStartedState (dval nrepl)))
        (is (= 42 (dval conn)))
        (is (= 42 (dval test-conn))) ;; test-conn is explicitly started via "t-states"
        (mount/stop)))))

#?(:clj
  (deftest composing

    (testing "states provided to the top level should narrow down the scope for the whole composition"
      (let [scope [#'tapp.conf/config
                   #'tapp.example/nrepl
                   #'tapp.nyse/conn
                   #'mount.test.composable-fns/test-nrepl
                   #'mount.test.composable-fns/test-conn]
            states (-> (only scope)
                       (with-args {:a 42})
                       (except [#'mount.test.composable-fns/test-nrepl
                                #'mount.test.composable-fns/test-conn])
                       (swap-states {#'tapp.example/nrepl #'mount.test.composable-fns/test-nrepl})
                       (swap {#'tapp.conf/config {:datomic {:uri "datomic:mem://composable-mount"}}}))]
        (is (= #{"#'tapp.nyse/conn" "#'tapp.conf/config" "#'tapp.example/nrepl"} (set states)))
        (mount/start states)
        (is (= {:a 42} (mount/args)))
        (is (= {:datomic {:uri "datomic:mem://composable-mount"}} (dval config)))
        (is (instance? datomic.peer.LocalConnection (dval conn)))
        (is (vector? (dval nrepl)))
        (mount/stop)))

    (testing "should compose and start in a single composition"
      (let [scope [#'tapp.conf/config
                   #'tapp.example/nrepl
                   #'tapp.nyse/conn
                   #'mount.test.composable-fns/test-nrepl
                   #'mount.test.composable-fns/test-conn]]
        (-> (only scope)
            (with-args {:a 42})
            (except [#'mount.test.composable-fns/test-nrepl
                     #'mount.test.composable-fns/test-conn])
            (swap-states {#'tapp.example/nrepl #'mount.test.composable-fns/test-nrepl})
            (swap {#'tapp.conf/config {:datomic {:uri "datomic:mem://composable-mount"}}})
            mount/start)
        (is (= {:a 42} (mount/args)))
        (is (= {:datomic {:uri "datomic:mem://composable-mount"}} (dval config)))
        (is (instance? datomic.peer.LocalConnection (dval conn)))
        (is (vector? (dval nrepl)))
        (mount/stop)))))
