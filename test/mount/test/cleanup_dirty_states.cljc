(ns mount.test.cleanup-dirty-states
  (:require
    #?@(:cljs [[cljs.test :as t :refer-macros [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer-macros [defstate]]
               [app.websockets :refer [system-a]]
               [app.conf :refer [config]]
               [app.audit-log :refer [log]]]
        :clj  [[clojure.test :as t :refer [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer [defstate]]
               [app.example]])
   [mount.test.helper :refer [dval helper forty-two]]))

#?(:clj
  (deftest cleanup-dirty-states
    (let [_ (mount/start)]
      (is (not (.isClosed (:server-socket (dval app.example/nrepl)))))
      (require 'app.example :reload)
      (mount/start)    ;; should not result in "BindException Address already in use" since the clean up will stop the previous instance
      (is (not (.isClosed (:server-socket (dval app.example/nrepl)))))
      (mount/stop)
      (is (instance? mount.core.NotStartedState (dval app.example/nrepl))))))

#?(:cljs
  (deftest cleanup-dirty-states
    (let [_ (mount/start #'mount.test.helper/helper)]
      (is (= :started (dval helper)))
      (is (= 42 @forty-two))
      (.require js/goog "mount.test.helper")                 ;; should have run :stop of `helper`
      ;; (is (= :cleaned @forty-two))                        ;; TODO: figure out how to reload a namespace properly
      ;; (is (instance? mount.core.NotStartedState (dval helper)))
      (mount/start #'mount.test.helper/helper) 
      (is (= :started (dval helper)))
      (mount/stop)
      (is (instance? mount.core.NotStartedState (dval helper))))))
