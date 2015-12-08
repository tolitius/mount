(ns mount.start-with-test
  (:require [mount.core :as mount :refer [defstate]]
            [app.conf :refer [config]]
            [app.nyse :refer [conn]]
            [app.example :refer [nrepl]]
            [clojure.test :refer :all]))

(defstate test-conn :start 42
                    :stop #(constantly 0))

(defstate test-nrepl :start [])

(deftest start-with

  (testing "should start with substitutes"
    (let [_ (mount/start-with {#'app.nyse/conn #'mount.start-with-test/test-conn
                               #'app.example/nrepl #'mount.start-with-test/test-nrepl})]
      (is (map? config))
      (is (vector? nrepl))
      (is (= conn 42))
      (mount/stop)))
  
  (testing "should not start the substitute itself"
    (let [_ (mount/start-with {#'app.nyse/conn #'mount.start-with-test/test-conn})]
      (is (instance? mount.core.NotStartedState test-conn))
      (is (= conn 42))
      (mount/stop)))

  (testing "should start normally after start-with"
    (let [_ (mount/start)]
      (is (map? config))
      (is (instance? clojure.tools.nrepl.server.Server nrepl))
      (is (instance? datomic.peer.LocalConnection conn))
      (is (= test-conn 42))
      (is (vector? test-nrepl))
      (mount/stop)))

  (testing "should start-without normally after start-with"
    (let [_ (mount/start-without #'mount.start-with-test/test-conn
                                 #'mount.start-with-test/test-nrepl)]
      (is (map? config))
      (is (instance? clojure.tools.nrepl.server.Server nrepl))
      (is (instance? datomic.peer.LocalConnection conn))
      (is (instance? mount.core.NotStartedState test-conn))
      (is (instance? mount.core.NotStartedState test-nrepl))
      (mount/stop))))

