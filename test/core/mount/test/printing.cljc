(ns mount.test.printing
  (:require
   #?@(:cljs [[cljs.test :as t :refer-macros [is are deftest testing use-fixtures]]
              [mount.core :as mount :refer-macros [defstate]]]
       :clj  [[clojure.test :as t :refer [is are deftest testing use-fixtures]]
              [mount.core :as mount :refer [defstate]]])))

#?(:clj (alter-meta! *ns* assoc ::load false))

(defstate foo
  :start (do (println "Starting!") 42))

(deftest test-printing-has-no-side-effects
  ;; Test that printing an unstarted DerefableState does not have the
  ;; side-effect of starting it
  (println foo)
  (is (not= 42 foo)))
