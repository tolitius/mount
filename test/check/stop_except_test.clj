(ns check.stop-except-test
  (:require [mount.core :as mount :refer [defstate]]
            [app.config :refer [app-config]]
            [app.nyse :refer [conn]]
            [app :refer [nrepl]]
            [clojure.test :refer :all]))

(deftest stop-except

  (testing "should stop all except nrepl"
    (let [_ (mount/start)
          _ (mount/stop-except #'app.nyse/conn #'app.config/app-config)]
      (is (map? app-config))
      (is (instance? datomic.peer.LocalConnection conn))
      (is (instance? mount.core.NotStartedState nrepl))
      (mount/stop)))
  
  (testing "should start normally after stop-except"
    (let [_ (mount/start)]
      (is (map? app-config))
      (is (instance? clojure.tools.nrepl.server.Server nrepl))
      (is (instance? datomic.peer.LocalConnection conn))
      (mount/stop)))

  (testing "should stop all normally after stop-except"
    (let [_ (mount/start)
          _ (mount/stop-except #'app.nyse/conn #'app.config/app-config)
          _ (mount/stop)]
      (is (instance? mount.core.NotStartedState app-config))
      (is (instance? mount.core.NotStartedState conn))
      (is (instance? mount.core.NotStartedState nrepl)))))
