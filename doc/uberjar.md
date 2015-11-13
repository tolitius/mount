## Creating Reloadable Uberjar'able App

### App state

Here is an example [app](https://github.com/tolitius/mount/tree/uberjar/test/app) that has these states:

```clojure
16:20:44.997 [nREPL-worker-0] INFO  mount - >> starting..  app-config
16:20:44.998 [nREPL-worker-0] INFO  mount - >> starting..  conn
16:20:45.393 [nREPL-worker-0] INFO  mount - >> starting..  nyse-app
16:20:45.443 [nREPL-worker-0] INFO  mount - >> starting..  nrepl
```

where `nyse-app` is _the_ app. It has the usual routes:

```clojure
(defroutes mount-example-routes

  (GET "/" [] "welcome to mount sample app!")
  (GET "/nyse/orders/:ticker" [ticker]
       (generate-string (find-orders ticker)))

  (POST "/nyse/orders" [ticker qty bid offer] 
        (add-order ticker (bigdec bid) (bigdec offer) (Integer/parseInt qty))
        (generate-string {:added {:ticker ticker 
                                  :qty qty 
                                  :bid bid 
                                  :offer offer}})))
```

and the reloadable state:

```clojure
(defn start-nyse []
  (create-nyse-schema)                      ;; creating schema (usually done long before the app is started..)
  (-> (routes mount-example-routes)
      (handler/site)
      (run-jetty {:join? false
                  :port (get-in app-config [:www :port])})))

(defstate nyse-app :start (start-nyse)
                   :stop (.stop nyse-app))  ;; it's a "org.eclipse.jetty.server.Server" at this point
```

In order not to block, and being reloadable, the Jetty server is started in `:join? false` mode which starts the server, 
and just returns a reference to it, so it can be easily stopped by `(.stop server)`

### "Uberjar is the :main"

In order for a standalone jar to run, it needs an entry point. This sample app [has one](https://github.com/tolitius/mount/blob/uberjar/test/app/app.clj#L16):

```clojure
;; example of an app entry point
(defn -main [& args]
  (mount/start))
```

And some usual suspects from `project.clj`:

```clojure
;; "test" is in sources here to just "demo" the uberjar without poluting mount "src"
:uberjar {:source-paths ["test/app"]
          :dependencies [[compojure "1.4.0"]
                          [ring/ring-jetty-adapter "1.1.0"]
                          [cheshire "5.5.0"]
                          [org.clojure/tools.nrepl "0.2.11"]
                          [com.datomic/datomic-free "0.9.5327" :exclusions [joda-time]]]
          :main app
          :aot :all}}
```

### REPL time

```clojure
$ lein do clean, repl

user=> (dev)(reset)
16:20:44.997 [nREPL-worker-0] INFO  mount - >> starting..  app-config
16:20:44.998 [nREPL-worker-0] INFO  mount - >> starting..  conn
16:20:45.393 [nREPL-worker-0] INFO  mount - >> starting..  nyse-app

16:20:45.442 [nREPL-worker-0] INFO  o.e.jetty.server.AbstractConnector - Started SelectChannelConnector@0.0.0.0:53600

16:20:45.443 [nREPL-worker-0] INFO  mount - >> starting..  nrepl
:ready
dev=>
```

Jetty server is started and ready to roll. And everything is still restartable:

```clojure
16:44:16.625 [nREPL-worker-2] INFO  mount - << stopping..  nrepl
16:44:16.626 [nREPL-worker-2] INFO  mount - << stopping..  nyse-app
16:44:16.711 [nREPL-worker-2] INFO  mount - << stopping..  conn
16:44:16.713 [nREPL-worker-2] INFO  mount - << stopping..  app-config

16:44:16.747 [nREPL-worker-2] INFO  mount - >> starting..  app-config
16:44:16.748 [nREPL-worker-2] INFO  mount - >> starting..  conn
16:44:16.773 [nREPL-worker-2] INFO  mount - >> starting..  nyse-app

16:44:16.777 [nREPL-worker-2] INFO  o.e.jetty.server.AbstractConnector - Started SelectChannelConnector@0.0.0.0:54476

16:44:16.778 [nREPL-worker-2] INFO  mount - >> starting..  nrepl
```

Notice the Jetty port difference between reloads: `53600` vs. `54476`. This is done on purpose via [config](https://github.com/tolitius/mount/blob/uberjar/test/resources/config.edn#L4):

```clojure
:www {:port 0}  ;; Jetty will randomly assign the available port (this is good for dev reloadability)
```

This of course can be solidified for different env deployments. For example I like `4242` :)

### 

```clojure
$ lein do clean, uberjar
...
Created /Users/tolitius/1/fun/mount/target/mount-0.1.0-SNAPSHOT-standalone.jar ;;  your version may vary
```

Let's give it a spin:

```bash
$ java -jar target/mount-0.1.0-SNAPSHOT-standalone.jar
...
16:51:35.586 [main] DEBUG o.e.j.u.component.AbstractLifeCycle - STARTED SelectChannelConnector@0.0.0.0:54728
```

Up and running:

```clojure
$ curl -X POST -d "ticker=GOOG&qty=100&bid=665.51&offer=665.59" "http://localhost:54728/nyse/orders"                    (uberjar ✔)
{"added":{"ticker":"GOOG","qty":"100","bid":"665.51","offer":"665.59"}}
```

### Choices

There are multiple ways to start a web app. This above the most straighforward one: start server / stop server.

But depending on the requirements / architecture, the app can also have an entry point to `(mount/start)` 
via something like [:ring :init](https://github.com/weavejester/lein-ring#general-options)). Or the (mount/start) 
can go into the handler function, etc.
