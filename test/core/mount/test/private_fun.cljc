(ns mount.test.private-fun
  (:require
    #?@(:cljs [[cljs.test :as t :refer-macros [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer-macros [defstate]]]
        :clj  [[clojure.test :as t :refer [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer [defstate]]])

    [mount.test.fun-with-values :refer [private-f]]))

#?(:clj (alter-meta! *ns* assoc ::load false))

(use-fixtures :once
              #?(:cljs {:before #(mount/start #'mount.test.fun-with-values/private-f)
                        :after mount/stop}
                 :clj #((mount/start #'mount.test.fun-with-values/private-f)
                        (%)
                        (mount/stop))))

(deftest fun-with-values
  (is (= (@private-f 1) 42)))
