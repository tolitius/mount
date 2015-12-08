(ns mount.deref.fun-with-values-test
  (:require [mount.core :as mount :refer [defstate]]
            [clojure.test :refer :all]))

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

(defn with-fun-and-values [f]
  (mount/in-cljc-mode)
  (require :reload 'mount.deref.fun-with-values-test)
  (mount/start #'mount.deref.fun-with-values-test/scalar
               #'mount.deref.fun-with-values-test/fun
               #'mount.deref.fun-with-values-test/with-fun
               #'mount.deref.fun-with-values-test/with-partial
               #'mount.deref.fun-with-values-test/f-in-f
               #'mount.deref.fun-with-values-test/f-args
               #'mount.deref.fun-with-values-test/f-no-args-value
               #'mount.deref.fun-with-values-test/f-no-args
               #'mount.deref.fun-with-values-test/private-f
               #'mount.deref.fun-with-values-test/f-value)
  (f)
  (mount/stop)
  (mount/in-clj-mode))

(use-fixtures :each with-fun-and-values)

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
