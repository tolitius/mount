(ns mount.test-self-host
  (:require
    [cljs.test :as t]

    mount.test.fun-with-values
    mount.test.private-fun
    mount.test.printing
    ))

(t/run-tests
 'mount.test.fun-with-values
 'mount.test.private-fun
 'mount.test.printing
 )

(defn run-tests []
  (t/run-all-tests #"mount.test.*"))
