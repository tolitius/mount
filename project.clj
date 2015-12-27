(defproject mount "0.1.8-SNAPSHOT"
  :description "managing Clojure and ClojureScript app state since (reset)"
  :url "https://github.com/tolitius/mount"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src"]

  :dependencies [] ;; for visual clarity
  
  :profiles {:dev {:source-paths ["dev" "dev/clj" "test/clj"]
                   :dependencies [[org.clojure/clojure "1.7.0"]
                                  [org.clojure/clojurescript "1.7.170"]; :classifier "aot"]
                                  [datascript "0.13.3"]
                                  [compojure "1.4.0"]
                                  [ring/ring-jetty-adapter "1.1.0"]
                                  [cheshire "5.5.0"]
                                  [hiccups "0.3.0"]
                                  [com.andrewmcveigh/cljs-time "0.3.14"]
                                  [ch.qos.logback/logback-classic "1.1.3"]
                                  [org.clojure/tools.logging "0.3.1"]
                                  [robert/hooke "1.3.0"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/tools.nrepl "0.2.11"]
                                  [com.datomic/datomic-free "0.9.5327" :exclusions [joda-time]]]

                   :plugins [[lein-cljsbuild "1.1.1"]
                             [lein-ring "0.9.7"]
                             [lein-doo "0.1.6"]
                             [lein-figwheel "0.5.0-2"]
                             [test2junit "1.1.3"]]

                   :ring {:handler app.www/mount-example-routes
                          :init mount.core/start
                          :port 4242}

                   :test2junit-output-dir ~(or (System/getenv "CIRCLE_TEST_REPORTS") "target/test2junit")

                   :clean-targets ^{:protect false} [:target-path
                                                     [:cljsbuild :builds :dev :compiler :output-dir]
                                                     [:cljsbuild :builds :prod :compiler :output-to]]
                   :cljsbuild {
                    :builds {:dev
                             {:source-paths ["src" "dev/cljs"]
                              :figwheel true

                              :compiler {:main app.example
                                         :asset-path "js/compiled/out"
                                         :output-to "dev/resources/public/js/compiled/mount.js"
                                         :output-dir "dev/resources/public/js/compiled/out"
                                         :optimizations :none
                                         :source-map true
                                         :source-map-timestamp true}}
                            :test
                             {:source-paths ["src" "dev/cljs" "test"]
                              :compiler {:main mount.test
                                         ;; :asset-path "js/compiled/out"
                                         :output-to "dev/resources/public/js/compiled/mount.js"
                                         :output-dir "dev/resources/public/js/compiled/test"
                                         :optimizations :none
                                         :source-map true
                                         :source-map-timestamp true}}
                             :prod
                             {:source-paths ["src" "dev/cljs"]
                              :compiler {:output-to "dev/resources/public/js/compiled/mount.js"
                                         :optimizations :advanced
                                         :pretty-print false}}}}}

             :test {:source-paths ["test/core" "test/clj" "test/cljs"]}})
