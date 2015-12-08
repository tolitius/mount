(ns mount.private-fun-test
  (:require [mount.core :as mount :refer [defstate]]
            [mount.fun-with-values-test :refer [private-f]]
            [clojure.test :refer :all]))

(defn with-fun-and-values [f]
  (mount/start #'mount.fun-with-values-test/private-f)
  (f)
  (mount/stop))

(use-fixtures :each with-fun-and-values)

(deftest fun-with-valuesj
  (is (= (private-f 1) 42)))
