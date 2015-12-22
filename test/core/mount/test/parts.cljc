(ns mount.test.parts
  (:require
    #?@(:cljs [[cljs.test :as t :refer-macros [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer-macros [defstate]]
               [tapp.websockets :refer [system-a]]
               [tapp.conf :refer [config]]
               [tapp.audit-log :refer [log]]]
        :clj  [[clojure.test :as t :refer [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer [defstate]]
               [tapp.nyse :refer [conn]]])
   [mount.test.helper :refer [dval]]))

#?(:clj (alter-meta! *ns* assoc ::load false))

(defstate should-not-start :start (constantly 42))

#?(:clj
  (defn with-parts [f]
    (mount/start #'tapp.conf/config #'tapp.nyse/conn)
    (f)
    (mount/stop)))

(use-fixtures :once 
              #?(:cljs {:before #(mount/start #'tapp.conf/config #'tapp.audit-log/log)
                        :after mount/stop}
                 :clj with-parts))

#?(:clj
  (deftest start-only-parts 
    (is (instance? datomic.peer.LocalConnection (dval conn)))
    (is (instance? mount.core.NotStartedState (dval should-not-start)))))

#?(:cljs
  (deftest start-only-parts 
    (is (instance? datascript.db/DB @(dval log)))
    (is (map? (dval config)))
    (is (instance? mount.core.NotStartedState (dval should-not-start)))
    (is (instance? mount.core.NotStartedState (dval system-a)))))
