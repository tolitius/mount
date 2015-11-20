(ns check.suspend-resume-test
  (:require [mount.core :as mount :refer [defstate]]
            [app.config :refer [app-config]]
            [app.nyse :refer [conn]]
            [app :refer [nrepl]]
            [clojure.test :refer :all]))

(deftest suspendable

  ;; lifecycle
  (testing "should suspend _only suspendable_ states that are currently started")
  (testing "should resume _only suspendable_ states that are currently suspended")
  (testing "should start all the states, except the ones that are currently suspended, should resume them instead")
  (testing "should stop all: started and suspended")

  ;; start-with
  (testing "when replacing a non suspendable state with a suspendable one,
            the later should be able to suspend/resume,
            the original should not be suspendable after resume and preserve its lifecycle fns after rollback/stop")

  (testing "when replacing a suspended state with a non suspendable one,
            the later should not be suspendable,
            the original should still be suspended and preserve its lifecycle fns after the rollback/stop")

  (testing "when replacing a suspended state with a suspendable one,
            the later should be suspendable,
            the original should still be suspended and preserve its lifecycle fns after the rollback/stop"))
