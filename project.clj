(defproject mount "0.1.4-SNAPSHOT"
  :description "managing Clojure app state since (reset)"
  :url "https://github.com/tolitius/mount"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.macro "0.1.2"]
                 [org.clojure/tools.namespace "0.2.11"]]
  
  :profiles {:dev {:source-paths ["dev" "test/app"]
                   :dependencies [[yesql "0.5.1"]
                                  [compojure "1.4.0"]
                                  [ring/ring-jetty-adapter "1.1.0"]
                                  [cheshire "5.5.0"]
                                  [org.clojure/tools.nrepl "0.2.11"]
                                  [com.datomic/datomic-free "0.9.5327" :exclusions [joda-time]]]}

             ;; "test" is in sources here to just "demo" the uberjar without poluting mount "src"
             :uberjar {:source-paths ["test/app"]
                       :dependencies [[compojure "1.4.0"]
                                      [ring/ring-jetty-adapter "1.1.0"]
                                      [cheshire "5.5.0"]
                                      [org.clojure/tools.nrepl "0.2.11"]
                                      [com.datomic/datomic-free "0.9.5327" :exclusions [joda-time]]]
                       :main app
                       :aot :all}})
