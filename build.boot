(set-env!
  :source-paths #{"src"}
  :dependencies '[;; mount brings _no dependencies_, everything here is for
                  ;; mount dev, examples apps and tests

                  [org.clojure/clojure "1.7.0"              :scope "provided"]
                  [org.clojure/clojurescript "1.7.189"      :scope "provided" :classifier "aot"]
                  [datascript "0.13.3"                      :scope "provided"]
                  [compojure "1.4.0"                        :scope "provided"]
                  [ring/ring-jetty-adapter "1.1.0"          :scope "provided"]
                  [cheshire "5.5.0"                         :scope "provided"]
                  [hiccups "0.3.0"                          :scope "provided" :exclusions [org.clojure/clojurescript]]
                  [com.andrewmcveigh/cljs-time "0.3.14"     :scope "provided"]
                  [ch.qos.logback/logback-classic "1.1.3"   :scope "provided"]
                  [org.clojure/tools.logging "0.3.1"        :scope "provided"]
                  [robert/hooke "1.3.0"                     :scope "provided"]
                  [org.clojure/tools.namespace "0.2.11"     :scope "provided"]
                  [org.clojure/tools.nrepl "0.2.12"         :scope "provided"]
                  [com.datomic/datomic-free "0.9.5327"      :scope "provided" :exclusions [joda-time]]

                  ;; boot clj
                  [boot/core              "2.5.1"           :scope "provided"]
                  [adzerk/bootlaces       "0.1.13"          :scope "test"]
                  [adzerk/boot-logservice "1.0.1"           :scope "test"]
                  [adzerk/boot-test       "1.0.6"           :scope "test"]

                  ;; boot cljs
                  [adzerk/boot-cljs            "1.7.170-3"       :scope "test"]
                  [adzerk/boot-cljs-repl       "0.3.0"           :scope "test"]
                  [pandeiro/boot-http          "0.7.1-SNAPSHOT"  :scope "test"]
                  [com.cemerick/piggieback     "0.2.1"           :scope "test" :exclusions [org.clojure/clojurescript]]
                  [weasel                      "0.7.0"           :scope "test" :exclusions [org.clojure/clojurescript]]
                  [adzerk/boot-reload          "0.4.2"           :scope "test"]
                  [crisptrutski/boot-cljs-test "0.2.1-SNAPSHOT"  :scope "test"]])

(require '[adzerk.bootlaces :refer :all]
         '[adzerk.boot-test :as bt]
         '[adzerk.boot-logservice :as log-service]
         '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[adzerk.boot-reload :refer [reload]]
         '[pandeiro.boot-http :refer :all]
         '[crisptrutski.boot-cljs-test :refer [test-cljs]]
         '[clojure.tools.logging :as log]
         '[clojure.tools.namespace.repl :refer [set-refresh-dirs]])

(def +version+ "0.1.7-SNAPSHOT")

(bootlaces! +version+)

(def log4b
  [:configuration
   [:appender {:name "STDOUT" :class "ch.qos.logback.core.ConsoleAppender"}
    [:encoder [:pattern "%-5level %logger{36} - %msg%n"]]]
   [:root {:level "TRACE"}
    [:appender-ref {:ref "STDOUT"}]]])

(deftask dev []
  (set-env! :source-paths #(conj % "dev/clj" "dev/cljs"))
  (set-env! :resource-paths #{"dev/resources"})

  (alter-var-root #'log/*logger-factory* 
                  (constantly (log-service/make-factory log4b)))
  (apply set-refresh-dirs (get-env :directories))
  (load-data-readers!)

  (require 'dev)
  (in-ns 'dev))

(deftask test []
  (set-env! :source-paths #(conj % "test/core" "test/clj")) ;; (!) :source-paths must not overlap.
  (bt/test))

(deftask cljs-example 
  "mount cljs example"
  []
  (set-env! :source-paths #(conj % "dev/clj" "dev/cljs"))
  (set-env! :resource-paths #{"dev/resources"})

  (comp
    (wait)
    (serve :dir "dev/resources/public/")
    (cljs-repl)
    (cljs :optimizations :advanced :ids #{"mount"})))

(task-options!
  push #(-> (into {} %) (assoc :ensure-branch nil))
  pom {:project     'mount
       :version     +version+
       :description "managing Clojure and ClojureScript app state since (reset)"
       :url         "https://github.com/tolitius/mount"
       :scm         {:url "https://github.com/tolitius/mount"}
       :license     {"Eclipse Public License"
                     "http://www.eclipse.org/legal/epl-v10.html"}})
