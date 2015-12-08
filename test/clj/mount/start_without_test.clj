(ns mount.start-without-test
  (:require [mount.core :as m]
            [app.conf :refer [config]]
            [app.nyse :refer [conn]]
            [app.example :refer [nrepl]]
            [clojure.test :refer :all]))

(defn without [f]
  (m/start-without #'app.nyse/conn #'app.example/nrepl)
  (f)
  (m/stop))

(use-fixtures :each without)

(deftest start-without-states
  (is (map? config))
  (is (instance? mount.core.NotStartedState nrepl))
  (is (instance? mount.core.NotStartedState conn)))
