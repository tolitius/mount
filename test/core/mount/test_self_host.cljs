(ns mount.test-self-host
  (:require
    [cljs.test :as t]

    mount.test.fun-with-values
    mount.test.private-fun
    mount.test.printing
    mount.test.parts
    mount.test.cleanup-dirty-states
    mount.test.stop-except
    mount.test.start-without
    mount.test.start-with
    mount.test.start-with-states
    ))

(t/run-tests
 'mount.test.fun-with-values
 'mount.test.private-fun
 'mount.test.printing
 'mount.test.parts
 'mount.test.cleanup-dirty-states
 ;; 'mount.test.stop-except        ;; TODO: can't run with deps.edn (due to "WebSocket is not defined")
 ;; 'mount.test.start-with         ;;       boot, lein have no problems
 ;; 'mount.test.start-with-states  ;;       most likely somm misconfigured in node..
 'mount.test.start-without
 )

(defn run-tests []
  (t/run-all-tests #"mount.test.*"))
