(ns mount.test.parts
  (:require
    #?@(:cljs [[cljs.test :as t :refer-macros [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer-macros [defstate]]
               [app.websockets :refer [system-a]]
               [app.conf :refer [config]]
               [app.audit-log :refer [log]]]
        :clj  [[clojure.test :as t :refer [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer [defstate]]
               [app.nyse :refer [conn]]])
   [mount.test.helper :refer [dval]]))

(defstate should-not-start :start (constantly 42))

#?(:clj
  (defn with-parts [f]
    (mount/start #'app.conf/config #'app.nyse/conn)
    (f)
    (mount/stop)))

(use-fixtures :once 
              #?(:cljs {:before #(mount/start #'app.conf/config #'app.audit-log/log)
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
