(ns check.fun-with-values-test
  (:require [mount.core :as mount :refer [defstate]]
            [clojure.test :refer :all]))

(defn f [n]
  (fn [m]
    (+ n m)))

(defn g [a b]
  (+ a b))

(defn- pf [n]
  (+ 41 n))

(defstate scalar :start 42)
(defstate fun :start #(inc 41))
(defstate with-fun :start (inc 41))
(defstate with-partial :start (partial g 41))
(defstate f-in-f :start (f 41))
(defstate f-args :start g)
(defstate f-value :start (g 41 1))
(defstate private-f :start pf)

(defn with-fun-and-values [f]
  (mount/start #'check.fun-with-values-test/scalar
               #'check.fun-with-values-test/fun
               #'check.fun-with-values-test/with-fun
               #'check.fun-with-values-test/with-partial
               #'check.fun-with-values-test/f-in-f
               #'check.fun-with-values-test/f-args
               #'check.fun-with-values-test/private-f
               #'check.fun-with-values-test/f-value)
  (f)
  (mount/stop))

(use-fixtures :each with-fun-and-values)

(deftest fun-with-values
  (is (= scalar 42))
  (is (= (fun) 42))
  (is (= with-fun 42))
  (is (= (with-partial 1) 42))
  (is (= (f-in-f 1) 42))
  (is (= (f-args 41 1) 42))
  (is (= (private-f 1) 42))
  (is (= f-value 42)))
