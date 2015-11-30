(ns check.fn-as-state-val-test
  (:require [clojure.test :refer :all]
            [mount.core :as m :refer (defstate)]))

(defn- mk-ring-handler []
  (fn [request]
    "Hello world!"))

(defstate ring-handler
  :start (m/fn-state (mk-ring-handler)))

(deftest fn-state-test

  (testing "Var ring-handler should hold a function when started."
    (try
      (m/start #'ring-handler)
      (is (fn? ring-handler))
      (is (= (ring-handler nil) "Hello world!"))
      (finally
        (m/stop)))))
