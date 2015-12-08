(ns mount.test
  (:require
    #?@(:cljs [[cljs.test :as t]
               [doo.runner :refer-macros [doo-tests]]]
        :clj  [[clojure.test :as t]])
    [mount.core :as mount]
    
    mount.test.fun-with-values
    mount.test.private-fun))

(mount/in-cljc-mode)

#?(:cljs
    (doo-tests 'mount.test.fun-with-values
               'mount.test.private-fun))

;; (doo.runner/do-all-tests)

(defn run-tests []
  (t/run-all-tests #"mount.test.*"))
