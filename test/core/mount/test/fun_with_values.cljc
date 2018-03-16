(ns mount.test.fun-with-values
  (:require
    #?@(:cljs [[cljs.test :as t :refer-macros [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer-macros [defstate]]]
        :clj  [[clojure.test :as t :refer [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer [defstate]]])))

#?(:clj (alter-meta! *ns* assoc ::load false))

(defn f [n]
  (fn [m]
    (+ n m)))

(defn g [a b]
  (+ a b))

(defn- pf [n]
  (+ 41 n))

(defn fna []
  42)

(defstate scalar :start 42)
(defstate fun :start #(inc 41))
(defstate with-fun :start (inc 41))
(defstate with-partial :start (partial g 41))
(defstate f-in-f :start (f 41))
(defstate f-no-args-value :start (fna))
(defstate f-no-args :start fna)
(defstate f-args :start g)
(defstate f-value :start (g 41 1))
(defstate private-f :start pf)

(defn start-states []
  (mount/start #'mount.test.fun-with-values/scalar
               #'mount.test.fun-with-values/fun
               #'mount.test.fun-with-values/with-fun
               #'mount.test.fun-with-values/with-partial
               #'mount.test.fun-with-values/f-in-f
               #'mount.test.fun-with-values/f-args
               #'mount.test.fun-with-values/f-no-args-value
               #'mount.test.fun-with-values/f-no-args
               #'mount.test.fun-with-values/private-f
               #'mount.test.fun-with-values/f-value))

(use-fixtures :once
              #?(:cljs {:before start-states
                        :after mount/stop}
                 :clj #((start-states) (%) (mount/stop))))

(deftest fun-with-values
  (is (= @scalar 42))
  (is (= (@fun) 42))
  (is (= @with-fun 42))
  (is (= (@with-partial 1) 42))
  (is (= (@f-in-f 1) 42))
  (is (= @f-no-args-value 42))
  (is (= (@f-no-args) 42))
  (is (= (@f-args 41 1) 42))
  (is (= (@private-f 1) 42))
  (is (= @f-value 42)))
