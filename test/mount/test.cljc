(ns mount.test
  (:require
    #?@(:cljs [[cljs.test :as t]
               [doo.runner :refer-macros [doo-tests]]]
        :clj  [[clojure.test :as t]])
    mount.core

    mount.test.fun-with-values
    mount.test.private-fun
    mount.test.parts
    ))

(mount.core/in-cljc-mode)

#?(:cljs

    ;; (doo.runner/do-all-tests)
    (doo-tests
               'mount.test.fun-with-values
               'mount.test.private-fun
               'mount.test.parts
               ))

(defn run-tests []
  (t/run-all-tests #"mount.test.*"))
