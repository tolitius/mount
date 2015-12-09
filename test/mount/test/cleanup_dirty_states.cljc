(ns mount.test.cleanup_dirty_states
  (:require
    #?@(:cljs [[cljs.test :as t :refer-macros [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer-macros [defstate]]
               [app.websockets :refer [system-a]]
               [app.conf :refer [config]]
               [app.audit-log :refer [log]]]
        :clj  [[clojure.test :as t :refer [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer [defstate]]
               [app.example]])
   [mount.test.helper :refer [dval]]))

#?(:clj
  (deftest cleanup-dirty-states
    (let [_ (mount/start)]
      (is (not (.isClosed (:server-socket (dval app.example/nrepl)))))
      (require 'app.example :reload)
      (mount/start)    ;; should not result in "BindException Address already in use" since the clean up will stop the previous instance
      (is (not (.isClosed (:server-socket (dval app.example/nrepl)))))
      (mount/stop)
      (is (instance? mount.core.NotStartedState (dval app.example/nrepl))))))
