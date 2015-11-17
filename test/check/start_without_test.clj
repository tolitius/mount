(ns check.start-without-test
  (:require [mount.core :as m]
            [app.config :refer [app-config]]
            [app.nyse :refer [conn]]
            [app :refer [nrepl]]
            [clojure.test :refer :all]))

(defn without [f]
  (m/start-without #'app.nyse/conn #'app/nrepl)
  (f)
  (m/stop))

(use-fixtures :each without)

(deftest start-without-states
  (is (map? app-config))
  (is (instance? mount.core.NotStartedState nrepl))
  (is (instance? mount.core.NotStartedState conn)))
