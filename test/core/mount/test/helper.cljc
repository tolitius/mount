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

(def counter (atom {:a {:started 0 :stopped 0}
                    :b {:started 0 :stopped 0}
                    :c {:started 0 :stopped 0}}))

(defn inc-counter [state status]
  (swap! counter update-in [state status] inc)
  status)
