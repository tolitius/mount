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
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [mount.app :refer :all]
            [mount :as app]))

(defn start []
  (app/start))

(defn stop []
  (app/stop))

(defn go
  "Initializes and starts the system running."
  []
  ;; (refresh) would redefine "defstates" which will call "start" on them
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after 'dev/go))
