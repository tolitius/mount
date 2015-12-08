(ns mount.cleanup_dirty_states_test
  (:require [mount.core :as mount]
            [app.example]
            [clojure.test :refer :all]))

(deftest cleanup-dirty-states
  (let [_ (mount/start)]
    (is (not (.isClosed (:server-socket app.example/nrepl))))
    (require 'app.example :reload)
    (mount/start)    ;; should not result in "BindException Address already in use" since the clean up will stop the previous instance
    (is (not (.isClosed (:server-socket app.example/nrepl))))
    (mount/stop)
    (is (instance? mount.core.NotStartedState app.example/nrepl))))
