(ns check.parts-test
  (:require [mount :refer [defstate] :as m]
            [app.nyse :refer [conn]]
            [clojure.test :refer :all]))

(defstate should-not-start :start (throw (RuntimeException. "should not have been started!")))

(defn with-parts [f]
  (m/start #'app.config/app-config #'app.nyse/conn)
  (f)
  (m/stop #'app.config/app-config #'app.nyse/conn))

(use-fixtures :each with-parts)

(deftest start-only-parts 
  (is (instance? datomic.peer.LocalConnection conn))
  (is (instance? mount.NotStartedState should-not-start)))
