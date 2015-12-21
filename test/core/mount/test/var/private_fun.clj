(ns mount.test.var.private-fun
  (:require [clojure.test :refer [is are deftest testing use-fixtures]]
            [mount.core :as mount :refer [defstate]]
            [mount.test.var.fun-with-values :refer [private-f]]))

(alter-meta! *ns* assoc ::load false)

(defn in-clj-mode [f]
  (mount/in-clj-mode)
  (require :reload 'mount.test.var.fun-with-values 'mount.test.var.private-fun)
  (mount/start #'mount.test.var.fun-with-values/private-f)
  (f)
  (mount/stop)
  (mount/in-cljc-mode))

(use-fixtures :once in-clj-mode)

(deftest fun-with-values
  (is (= (private-f 1) 42)))
