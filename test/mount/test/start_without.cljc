(ns mount.test.start-without
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

#?(:clj
  (defn without [f]
    (mount/start-without #'app.nyse/conn #'app.example/nrepl)
    (f)
    (mount/stop)))

  (use-fixtures :once 
                #?(:cljs {:before #(mount/start-without #'app.websockets/system-a #'app.audit-log/log)
                          :after mount/stop}
                   :clj without))

#?(:clj
  (deftest start-without-states
    (is (map? (dval config)))
    (is (instance? mount.core.NotStartedState (dval nrepl)))
    (is (instance? mount.core.NotStartedState (dval conn)))))
