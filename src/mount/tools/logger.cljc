(ns mount.tools.logger
  #?@(:cljs [(:require [goog.log :as glog])
             (:import [goog.debug Console])]))

#?(:cljs
    (defonce ^:dynamic *logger*
      (do
        (.setCapturing (Console.) true)
        (glog/getLogger "mount" nil))))

#?(:clj
    (defn log [msg & _]
      (prn msg)))

#?(:cljs
    (defn log [msg & level]
      (case (first level)
        :error (glog/error *logger* msg nil)
        (glog/info *logger* msg nil))))

