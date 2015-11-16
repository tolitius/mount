(ns check.start-with-test
  (:require [mount :refer [defstate] :as m]
            [app.config :refer [app-config]]
            [app.nyse :refer [conn]]
            [app :refer [nrepl]]
            [clojure.test :refer :all]))

(defstate test-conn :start (long 42)
                    :stop (constantly 0))

(defstate test-nrepl :start (vector))

(deftest start-with

  (testing "should start with substitutes"
    (let [_ (m/start-with {#'app.nyse/conn #'check.start-with-test/test-conn
                           #'app/nrepl #'check.start-with-test/test-nrepl})]
      (is (map? app-config))
      (is (vector? nrepl))
      (is (= conn 42))
      (mount/stop)))
  
  (testing "should start normally after start-with"
    (let [_ (m/start)]
      (is (map? app-config))
      (is (instance? clojure.tools.nrepl.server.Server nrepl))
      (is (instance? datomic.peer.LocalConnection conn))
      (mount/stop))))
