## Passing Runtime Arguments

This example lives in the `with-args` branch. If you'd like to follow along:

```bash
$ git checkout with-args
Switched to branch 'with-args'
```

## Start with args

In order to pass runtime arguments, these could be `-this x -that y` params or `-Dparam=` or 
just a path to an external configuration file, `mount` has a special `start-with-args` function:

```clojure
(defn -main [& args]
  (mount/start-with-args args))
```

Most of the time it is better to parse args before they "get in", so usually accepting args would look something like:

```clojure
(defn -main [& args]
  (mount/start-with-args
    (parse-args args)))
```

where the `parse-args` is an app specific function.

### Reading arguments

Once the arguments are passed to the app, they are available via:

```clojure
(mount/args)
```

Which, unless the arguments were parsed or modified in the `-main` function, 
will return the original `args` that were passed to `-main`.

### "Reading" example

Here is an [example app](https://github.com/tolitius/mount/blob/with-args/test/app/app.clj) that takes `-main` arguments
and parses them with [tools.cli](https://github.com/clojure/tools.cli):

```clojure
;; "any" regular function to pass arguments
(defn parse-args [args]
  (let [opts [["-d" "--datomic-uri [datomic url]" "Datomic URL"
              :default "datomic:mem://mount"]
              ["-h" "--help"]]]
    (-> (parse-opts args opts)
        :options)))

;; example of an app entry point with arguments
(defn -main [& args]
  (mount/start-with-args
    (parse-args args)))
```

For the example sake the app reads arguments in two places:

* [inside](https://github.com/tolitius/mount/blob/with-args/test/app/nyse.clj#L17) a `defstate`

```clojure
(defstate conn :start #(new-connection (mount/args))
               :stop #(disconnect (mount/args) conn))
```

* and from "any" [other place](https://github.com/tolitius/mount/blob/with-args/test/app/config.clj#L8) within a function:

```clojure
(defn load-config [path]
  ;; ...
  (if (:help (mount/args))
    (info "\n\nthis is a sample mount app to demo how to pass and read runtime arguments\n"))
  ;; ...)
```

### "Uber" example

In order to demo all of the above, we'll build an uberjar:

```bash
$ lein do clean, uberjar
...
Created .. mount/target/mount-0.1.5-SNAPSHOT-standalone.jar
```

Since we have a default for a Datomic URI, it'll work with no arguments:

```bash
$ java -jar target/mount-0.1.5-SNAPSHOT-standalone.jar

22:12:03.290 [main] INFO  mount - >> starting..  app-config
22:12:03.293 [main] INFO  mount - >> starting..  conn
22:12:03.293 [main] INFO  app.nyse - creating a connection to datomic: datomic:mem://mount
22:12:03.444 [main] INFO  mount - >> starting..  nrepl
```

Now let's ask it to help us:

```bash
$ java -jar target/mount-0.1.5-SNAPSHOT-standalone.jar --help

22:13:48.798 [main] INFO  mount - >> starting..  app-config
22:13:48.799 [main] INFO  app.config -

this is a sample mount app to demo how to pass and read runtime arguments

22:13:48.801 [main] INFO  mount - >> starting..  conn
22:13:48.801 [main] INFO  app.nyse - creating a connection to datomic: datomic:mem://mount
22:13:48.946 [main] INFO  mount - >> starting..  nrepl
```

And finally let's connect to the Single Malt Database. It's Friday..

```bash
$ java -jar target/mount-0.1.5-SNAPSHOT-standalone.jar -d datomic:mem://single-malt-database

22:16:10.733 [main] INFO  mount - >> starting..  app-config
22:16:10.737 [main] INFO  mount - >> starting..  conn
22:16:10.737 [main] INFO  app.nyse - creating a connection to datomic: datomic:mem://single-malt-database
22:16:10.885 [main] INFO  mount - >> starting..  nrepl
```

### Other usecases

Depending the requirements, these runtime arguments could take different shapes of forms. You would have a full control
over what is passed to the app, the same way you have it without mount through `-main [& args]`.
