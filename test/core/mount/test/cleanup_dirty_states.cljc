(ns mount.test.cleanup-dirty-states
  (:require
    #?@(:cljs [[cljs.test :as t :refer-macros [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer-macros [defstate]]
               [tapp.websockets :refer [system-a]]
               [tapp.conf :refer [config]]
               [tapp.audit-log :refer [log]]]
        :clj  [[clojure.test :as t :refer [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer [defstate]]
               [tapp.example]])
   [mount.test.helper :refer [dval helper forty-two]]))

#?(:clj (alter-meta! *ns* assoc ::load false))

#?(:clj
  (deftest cleanup-dirty-states
    (let [_ (mount/start)]
      (is (not (.isClosed (:server-socket (dval tapp.example/nrepl)))))
      (require 'tapp.example :reload)
      (mount/start)    ;; should not result in "BindException Address already in use" since the clean up will stop the previous instance
      (is (not (.isClosed (:server-socket (dval tapp.example/nrepl)))))
      (mount/stop)
      (is (instance? mount.core.NotStartedState (dval tapp.example/nrepl))))))

#?(:clj
  (deftest restart-on-recompile
    (let [_ (mount/start)
          before (:server-socket (dval tapp.example/nrepl))]
      (require 'tapp.example :reload)
      (is (not= before (:server-socket (dval tapp.example/nrepl))))  ;; should have restarted on recompile/reload, hence different reference
      (mount/stop)
      (is (instance? mount.core.NotStartedState (dval tapp.example/nrepl))))))

#?(:clj
  (deftest start-on-recompile
    (let [_ (mount/start)
          before (dval tapp.conf/config)]
      (require 'tapp.conf :reload)
      (is (not (identical? before (dval tapp.conf/config))))  ;; should be a newly recompiled map
      (mount/stop)
      (is (instance? mount.core.NotStartedState (dval tapp.conf/config))))))

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
