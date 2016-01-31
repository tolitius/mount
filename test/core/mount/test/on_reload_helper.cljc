(ns mount.test.on-reload-helper
  (:require
    #?@(:cljs [[cljs.test :as t :refer-macros [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer-macros [defstate]]
               [tapp.websockets :refer [system-a]]
               [tapp.conf :refer [config]]
               [tapp.audit-log :refer [log]]]
        :clj  [[clojure.test :as t :refer [is are deftest testing use-fixtures]]
               [mount.core :as mount :refer [defstate]]
               [tapp.example]])
   [mount.test.helper :refer [inc-counter]]))

(defstate ^{:on-reload :noop} a :start (inc-counter :a :started)
                                :stop (inc-counter :a :stopped))

(defstate ^{:on-reload :stop} b :start (inc-counter :b :started)
                                :stop (inc-counter :b :stopped))

(defstate c :start (inc-counter :c :started)
            :stop (inc-counter :c :stopped))

