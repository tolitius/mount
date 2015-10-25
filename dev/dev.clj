(ns dev
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  ;; (:use [cljs.repl :only [repl]]
  ;;       [cljs.repl.browser :only [repl-env]])
  (:require [clojure.java.io :as io]
            [clojure.java.javadoc :refer [javadoc]]
            [clojure.pprint :refer [pprint]]
            [clojure.reflect :refer [reflect]]
            [clojure.repl :refer [apropos dir doc find-doc pst source]]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :as test]
            ;; [clojure.core.async :refer [>!! <!! >! <! go-loop alt! timeout]]
            [clojure.tools.namespace.repl :as tn]
            [mount :as app]
            [app :refer :all]))  ;; <<<< replace this your "app" namespace(s) you want to be available at REPL time

(defn start []
  (app/start))

(defn stop []
  (app/stop))

(defn refresh []
  (stop)
  (tn/refresh))

(defn refresh-all []
  (stop)
  (tn/refresh-all))

(defn go
  "starts all defstate.s"
  []
  (start)
  :ready)

(defn reset
  "stops all defstates, reloads modified source files, and restarts defstates"
  []
  (stop)
  (tn/refresh :after 'dev/go))
