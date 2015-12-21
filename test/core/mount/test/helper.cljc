(ns mount.test.helper
  (:require
    #?@(:cljs [[mount.core :as mount :refer-macros [defstate]]]
        :clj  [[mount.core :as mount :refer [defstate]]])))

#?(:clj (alter-meta! *ns* assoc ::load false))

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
