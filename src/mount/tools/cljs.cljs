(ns mount.tools.cljs
  (:require [cljs.analyzer :as ana]
            [goog.string :as gstring]))

(defn this-ns [] 
  ana/*cljs-ns*)

(defn starts-with? [s pre]
  (gstring/startsWith s pre))
