(ns mount.tools.logger
  #?@(:cljs [(:require [goog.log :as glog])
             (:import [goog.debug Console])]))

#?(:cljs
    (defonce *logger*
      (do
        (.setCapturing (Console.) true)
        (glog/getLogger "mount"))))

#?(:clj
    (defn log [msg & _]
      (prn msg)))

#?(:cljs
    (defn log [msg & level]
      (case (first level)
        :error (glog/error *logger* msg)
        (glog/info *logger* msg))))

