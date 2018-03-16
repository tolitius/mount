(ns mount.test.start-without
  (:require
    #?@(:cljs [[cljs.test :as t :refer-macros [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer-macros [defstate]]
               [tapp.websockets :refer [system-a]]
               [tapp.conf :refer [config]]
               [tapp.audit-log :refer [log]]]
        :clj  [[clojure.test :as t :refer [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer [defstate]]
               [tapp.conf :refer [config]]
               [tapp.nyse :refer [conn]]
               [tapp.example :refer [nrepl]]])
   [mount.test.helper :refer [dval helper]]))

#?(:clj (alter-meta! *ns* assoc ::load false))

#?(:clj
  (defn without [f]
    (mount/start-without #'tapp.nyse/conn #'tapp.example/nrepl)
    (f)
    (mount/stop)))

  (use-fixtures :once
                #?(:cljs {:before #(mount/start-without #'mount.test.helper/helper #'tapp.websockets/system-a)
                          :after mount/stop}
                   :clj without))

#?(:clj
  (deftest start-without-states
    (is (map? (dval config)))
    (is (instance? mount.core.NotStartedState (dval nrepl)))
    (is (instance? mount.core.NotStartedState (dval conn)))))

#?(:cljs
  (deftest start-without-states
    (is (map? (dval config)))
    (is (instance? datascript.db/DB @(dval log)))
    (is (instance? mount.core.NotStartedState (dval helper)))
    (is (instance? mount.core.NotStartedState (dval system-a)))))
