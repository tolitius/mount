(ns check.cleanup_dirty_states_test
  (:require [mount.core :as mount]
            [app]
            [clojure.test :refer :all]))

(deftest cleanup-dirty-states
  (let [_ (mount/start)]
    (is (not (.isClosed (:server-socket app/nrepl))))
    (require 'app :reload)
    (mount/start)    ;; should not result in "BindException Address already in use" since the clean up will stop the previous instance
    (is (not (.isClosed (:server-socket app/nrepl))))
    (mount/stop)
    (is (instance? mount.core.NotStartedState app/nrepl))))
