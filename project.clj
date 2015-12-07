(defproject mount "0.1.6-SNAPSHOT"
  :description "managing Clojure app state since (reset)"
  :url "https://github.com/tolitius/mount"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]]
  
  :profiles {:dev {:source-paths ["dev" "test/app"]
                   :dependencies [[ch.qos.logback/logback-classic "1.1.3"]
                                  [org.clojure/tools.logging "0.3.1"]
                                  [robert/hooke "1.3.0"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/tools.nrepl "0.2.11"]
                                  [com.datomic/datomic-free "0.9.5327" :exclusions [joda-time]]]

                   :plugins [[lein-cljsbuild "1.1.1"]
                             [lein-figwheel "0.5.0-2"]]

                   :cljsbuild {
                    :builds [{:id "dev"
                              :source-paths ["src" "test"]
                              ;; :figwheel {:on-jsload "mount.example.cljs/on-js-reload"}

                              :compiler {:main mount.example.cljs
                                         :asset-path "js/compiled/out"
                                         :output-to "test/resources/public/js/compiled/mount.js"
                                         :output-dir "test/resources/public/js/compiled/out"
                                         :optimizations :none
                                         :source-map true
                                         :source-map-timestamp true
                                         :cache-analysis true }}
                             {:id "prod"
                              :source-paths ["src" "test"]
                              :compiler {:output-to "test/resources/public/js/compiled/mount.js"
                                         :optimizations :advanced
                                         :pretty-print false}}]}}})
