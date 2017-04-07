(ns core.mount.test.start-dependencies
  (:require
    #?@(:cljs [[cljs.test :as t :refer-macros [is deftest use-fixtures]]
               [mount.core :as mount :refer-macros [defstate]]]
        :clj  [[clojure.test :as t :refer [is deftest use-fixtures]]
               [mount.core :as mount :refer [defstate]]])
    [mount.test.helper :refer [dval]]))

#?(:clj (alter-meta! *ns* assoc ::load false))

(defstate dependency-1 :start 1)
(defstate dependency-2 :start 2)
(defstate thing-to-start
  :deps [#'dependency-1 #'dependency-2]
  :start 3)
(defstate should-not-start :start 4)

(defn- start-states []
  (mount/start #'dependency-1))

(use-fixtures :once
              #?(:cljs {:before start-states
                        :after mount/stop}
                 :clj #((start-states) (%) (mount/stop))))

(deftest dependencies-test
  (is (= {:started ["#'core.mount.test.start-dependencies/dependency-2" "#'core.mount.test.start-dependencies/thing-to-start"]}
         (mount/start #'thing-to-start)))
  (is (= 3 (dval thing-to-start)))
  (is (= 2 (dval dependency-2)))
  (is (= 1 (dval dependency-1)))
  (is (instance? mount.core.NotStartedState (dval should-not-start))))
