(set-env!
  :source-paths #{"src"}
  :dependencies '[;; dev / examples / test
                  [org.clojure/clojure "1.7.0"              :scope "provided"]
                  [org.clojure/clojurescript "1.7.170"      :scope "provided"]
                  [datascript "0.13.3"                      :scope "provided"]
                  [compojure "1.4.0"                        :scope "provided"]
                  [ring/ring-jetty-adapter "1.1.0"          :scope "provided"]
                  [cheshire "5.5.0"                         :scope "provided"]
                  [hiccups "0.3.0"                          :scope "provided"]
                  [com.andrewmcveigh/cljs-time "0.3.14"     :scope "provided"]
                  [ch.qos.logback/logback-classic "1.1.3"   :scope "provided"]
                  [org.clojure/tools.logging "0.3.1"        :scope "provided"]
                  [robert/hooke "1.3.0"                     :scope "provided"]
                  [org.clojure/tools.namespace "0.2.11"     :scope "provided"]
                  [org.clojure/tools.nrepl "0.2.11"         :scope "provided"]
                  [com.datomic/datomic-free "0.9.5327"      :scope "provided" :exclusions [joda-time]]

                  ;; boot
                  [boot/core           "2.5.1"              :scope "provided"]
                  [adzerk/bootlaces    "0.1.13"             :scope "test"]
                  [adzerk/boot-test    "1.0.6"              :scope "test"]])

(require '[adzerk.bootlaces :refer :all]
         '[adzerk.boot-test :as bt])

(def +version+ "0.1.7-SNAPSHOT")

(bootlaces! +version+)

(deftask dev [] 

  (set-env! :source-paths #(conj % "dev/clj"))

  (defn in []
    (load-data-readers!)
    (require 'dev)
    (in-ns 'dev))

  (comp
    (watch)
    (repl)))

(deftask test []
  (set-env! :source-paths #(conj % "test" "test/clj")) ;; (!) :source-paths must not overlap.
  (bt/test))

(task-options!
  push #(-> (into {} %) (assoc :ensure-branch nil))
  pom {:project     'mount
       :version     +version+
       :description "managing Clojure and ClojureScript app state since (reset)"
       :url         "https://github.com/tolitius/mount"
       :scm         {:url "https://github.com/tolitius/mount"}
       :license     {"Eclipse Public License"
                     "http://www.eclipse.org/legal/epl-v10.html"}})
