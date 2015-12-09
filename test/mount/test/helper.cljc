(ns mount.test.helper
  (:require mount.core))

(defn dval 
  "returns a value of DerefableState without deref'ing it"
  [d]
  (-> (@@(var mount.core/meta-state) 
                #?(:clj (.name d)
                   :cljs (.-name d)))
             :inst
             deref))
