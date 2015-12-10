(ns mount.test.helper
  (:require
    #?@(:cljs [[mount.core :as mount :refer-macros [defstate]]]
        :clj  [[mount.core :as mount :refer [defstate]]])))

(defn dval 
  "returns a value of DerefableState without deref'ing it"
  [d]
  (-> (@@(var mount.core/meta-state) 
                #?(:clj (.name d)
                   :cljs (.-name d)))
             :inst
             deref))

(def forty-two (atom 42))

(defstate helper :start :started
                 :stop (reset! forty-two :cleaned))
