(ns check.deref.private-fun-test
  (:require [mount.core :as mount :refer [defstate]]
            [check.deref.fun-with-values-test :refer [private-f]]
            [clojure.test :refer :all]))

(defn with-fun-and-values [f]
  (mount/in-cljc-mode)
  (mount/start #'check.deref.fun-with-values-test/private-f)
  (f)
  (mount/stop)
  (mount/in-clj-mode))

(use-fixtures :each with-fun-and-values)

(deftest fun-with-valuesj
  (is (= (@private-f 1) 42)))
