(ns mount.test.on-reload
  (:require
    #?@(:cljs [[cljs.test :as t :refer-macros [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer-macros [defstate]]
               [tapp.websockets :refer [system-a]]
               [tapp.conf :refer [config]]
               [tapp.audit-log :refer [log]]]
        :clj  [[clojure.test :as t :refer [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer [defstate]]
               [tapp.example]])
    [mount.test.helper :refer [dval helper forty-two counter inc-counter]]
    [mount.test.on-reload-helper :refer [a b c]]))

#?(:clj (alter-meta! *ns* assoc ::load false))

#?(:clj
  (defn abc [f]
    (mount/start #'mount.test.on-reload-helper/a
                 #'mount.test.on-reload-helper/b
                 #'mount.test.on-reload-helper/c)
    (f)
    (mount/stop)))

(use-fixtures :each
              #?(:cljs {:before #(mount/start #'mount.test.on-reload-helper/a
                                              #'mount.test.on-reload-helper/b
                                              #'mount.test.on-reload-helper/c)
                        :after mount/stop}
                 :clj abc))

#?(:clj
    (deftest restart-by-default
      (is (= '(:started) (distinct (map dval [a b c]))))
      (let [pre-reload @counter]
        (require 'mount.test.on-reload-helper :reload)

        ;; "a" is marked as :noop on reload
        ;; previous behavior left a stale reference =>>> ;; (is (instance? mount.core.NotStartedState (dval a))) ;; (!) stale reference of old a is still there somewhere
        (is (= :started (dval a)))   ;; make sure a still has the same instance as before reload
        (is (= (-> pre-reload :a)    ;; and the start was not called: the counter did not change
               (-> @counter :a)))

        ;; "b" is marked as :stop on reload
        (is (instance? mount.core.NotStartedState (dval b)))
        (is (= (-> pre-reload :b :started)
               (-> @counter :b :started)))
        (is (= (inc (-> pre-reload :b :stopped))
               (-> @counter :b :stopped)))

        ;; "c" is not marked on reload, using "restart" as default
        (is (= :started (dval c)))
        (is (= (inc (-> pre-reload :c :started))
               (-> @counter :c :started)))
        (is (= (inc (-> pre-reload :c :stopped))
               (-> @counter :c :stopped))))))
