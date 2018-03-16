(ns mount.test.cleanup-deleted-states
  (:require
    #?@(:cljs [[cljs.test :as t :refer-macros [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer-macros [defstate]]
               [tapp.websockets :refer [system-a]]
               [tapp.conf :refer [config]]
               [tapp.audit-log :refer [log]]]
        :clj  [[clojure.test :as t :refer [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer [defstate]]
               [tapp.example]])
   [mount.test.helper :refer [dval helper forty-two]]))

(def status (atom :a-not-started))
(defstate a :start (reset! status :a-started)
            :stop (reset! status :a-stopped))

#?(:clj (alter-meta! *ns* assoc ::load false))

#?(:clj
    (deftest cleanup-deleted-state

      (testing "should start all and remove/delete state from ns"
        (let [started (-> (mount/start) :started set)]
          (is (some #{"#'mount.test.cleanup-deleted-states/a"}
                    started))
          (is (= :a-started @status))
          (ns-unmap 'mount.test.cleanup-deleted-states 'a)
          (is (nil? (resolve 'mount.test.cleanup-deleted-states/a)))))

      (testing "should cleanup/stop a state after it was deleted from ns"
          (is (empty? (:started (mount/start)))) ;; on any mount op (not necessarily on "stop")
          (is (= :a-stopped @status))
          (is (not (some #{"#'mount.test.cleanup-deleted-states/a"}
                         (keys @@#'mount.core/meta-state)))))

      (testing "should not stop it again on stop (should not be there by this point)")
          (is (not (some #{"#'mount.test.cleanup-deleted-states/a"}
                         (-> (mount/stop) :stopped set))))))

;; (t/run-tests)
