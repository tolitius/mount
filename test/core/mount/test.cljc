(ns mount.test
  (:require
    #?@(:cljs [[cljs.test :as t]
               [doo.runner :refer-macros [doo-tests]]]
        :clj  [[clojure.test :as t]])
    mount.core

    mount.test.fun-with-values
    mount.test.private-fun
    mount.test.parts
    mount.test.cleanup-dirty-states
    mount.test.stop-except
    mount.test.start-without
    mount.test.start-with
    mount.test.start-with-states
    mount.test.printing
    ))

#?(:clj (alter-meta! *ns* assoc ::load false))

(mount.core/in-cljc-mode)

#?(:cljs

    ;; (doo.runner/do-all-tests)
    (doo-tests
               'mount.test.fun-with-values
               'mount.test.private-fun
               'mount.test.parts
               'mount.test.cleanup-dirty-states
               'mount.test.stop-except
               'mount.test.start-without
               'mount.test.start-with
               'mount.test.start-with-states
               'mount.test.printing
               ))

(defn run-tests []
  (t/run-all-tests #"mount.test.*"))
