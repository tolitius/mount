(ns mount.parts-test
  (:require [mount.core :as mount :refer [defstate] :as m]
            [app.nyse :refer [conn]]
            [clojure.test :refer :all]))

(defstate should-not-start :start #(constantly 42))

(defn with-parts [f]
  (m/start #'app.conf/config #'app.nyse/conn)
  (f)
  (m/stop))

(use-fixtures :each with-parts)

(deftest start-only-parts 
  (is (instance? datomic.peer.LocalConnection conn))
  (is (instance? mount.core.NotStartedState should-not-start)))
