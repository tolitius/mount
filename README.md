# mount

Riding side by side with [tools.namespace](https://github.com/clojure/tools.namespace) to manage application state during development.

![Clojars Project](http://clojars.org/mount/latest-version.svg)

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [Why?](#why)
- [How](#how)
  - [Creating State](#creating-state)
  - [Using State](#using-state)
- [Dependencies](#dependencies)
  - [Talking States](#talking-states)
- [Start and Stop Order](#start-and-stop-order)
- [The Importance of Being Reloadable](#the-importance-of-being-reloadable)
- [Mount and Develop!](#mount-and-develop)
  - [Running New York Stock Exchange](#running-new-york-stock-exchange)
- [License](#license)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Why?

Clojure is 

* powerful 
* simple
* and _fun_

Depending on how application state is managed during development, the above three superpowers can either stay, 
go somewhat, or go completely.

If Clojure REPL (i.e. `lein repl`, `boot repl`) fired up instantly, the need to reload application state
inside the REPL would go away. But at the moment, and for some time in the future, managing state by making it
reloadable within the same REPL session is important to retain all the Clojure superpowers.

Here is a good [breakdown](http://blog.ndk.io/2014/02/25/clojure-bootstrapping.html) on the Clojure REPL 
startup time, and it is [not because of JVM](http://blog.ndk.io/2014/02/11/jvm-slow-startup.html).

`mount` is here to preserve all the Clojure superpowers while making _the application state_ enjoyably reloadable.

There is another Clojure superpower that `mount` is made to retain: Clojure community.
Pull request away, let's solve this thing!

## How

```clojure
(require '[mount :refer [defstate]])
```

### Creating State

Creating state is easy:

```clojure
(defstate conn :start (create-conn))
```

where `(create-conn)` is defined elsewhere, can be right above it.

In case this state needs to be cleaned / destryed between reloads, there is also `:stop`

```clojure
(defstate conn :start (create-conn)
               :stop (disconnect conn))
```

That is pretty much it. But wait, there is more.. this state is _a top level being_, which means it can be simply 
`required` by other namespaces.

### Using State

For example let's say an `app` needs a connection above. No problem:

```clojure
(ns app
  (:require [above :refer [conn]]))
```

where `above` is an arbitrary namespace that defines the above state / connection.

## Dependencies

If the whole app is one big application context (or `system`), cross dependencies with a solid dependency graph
is an integral part of the system.

But if a state is a simple top level being, these beings can coexist with each other and with other
namespaces by being `required` instead.

If a managing state library requires a whole app buy-in, where everything is a bean or a component, 
it is a framework, and  dependency graph is usually quite large and complex, 
since it has _everything_ (every piece of the application) in it. 

But if stateful things are kept lean and low level (i.e. I/O, queues, etc.), dependency graphs are simple 
and small, and everything else is just namespaces and functions: the way it should be.

### Talking States

There are of course direct dependecies that `mount` respects:

```clojure
(ns app.config
  (:require [mount :refer [defstate]]))

(defstate app-config 
  :start (load-config "test/resources/config.edn"))
```

this `app-config`, being top level, can be used in other namespaces, including the ones that create states:

```clojure
(ns app.database
  (:require [mount :refer [defstate]]
            [app.config :refer [app-config]]))

(defstate conn :start (create-connection app-config))
```

[here](https://github.com/tolitius/mount/blob/master/test/app/nyse.clj) 
is an example of a Datomic connection that "depends" on a similar `app-config`.

## The Importance of Being Reloadable

`mount` has start and stop functions that will walk all the states created with `defstate` and start / stop them
accordingly: i.e. will call their `:start` and `:stop` defined functions.

This can be easily hooked up to [tool.namespace](https://github.com/clojure/tools.namespace), to make the whole
application reloadable. Here is a [dev.clj](https://github.com/tolitius/mount/blob/master/dev/dev.clj) as 
an example, that sums up to:

```clojure
(defn go []
  (start)
  :ready)

(defn reset []
  (stop)
  (tn/refresh :after 'dev/go))
```

the `(reset)` is then used in REPL to restart / relaod application state without the need to restart the REPL itself.

## Start and Stop Order

Since dependencies are "injected" by `require`ing on the namespace level, `mount` trusts Clojure compiler to 
maintain the start order for all the `defstates`.

The "start" order is then recorded and replayed on each `(reset)`.

The "stop" order is simply `(reverse "start order")`:

```clojure
dev=> (reset)
08:21:39.430 [nREPL-worker-1] DEBUG mount - << stopping..  nrepl
08:21:39.431 [nREPL-worker-1] DEBUG mount - << stopping..  conn
08:21:39.432 [nREPL-worker-1] DEBUG mount - << stopping..  app-config

:reloading (app.config app.nyse app.utils.datomic app)

08:21:39.462 [nREPL-worker-1] DEBUG mount - >> starting..  app-config
08:21:39.463 [nREPL-worker-1] DEBUG mount - >> starting..  conn
08:21:39.481 [nREPL-worker-1] DEBUG mount - >> starting..  nrepl
:ready
```

You can see examples of start and stop flows in the [example app](https://github.com/tolitius/mount#mount-and-develop).

## Mount and Develop!

`mount` comes with an example [app](https://github.com/tolitius/mount/tree/master/test/app) 
that has 3 states:

* `config`, loaded from the files and refreshed on each `(reset)`
* `datamic connection` that uses the config to create itself
* `nrepl` that uses config to bind to host/port

### Running New York Stock Exchange

To try it out, clone `mount`, get to REPL and switch to `(dev)`:

```clojure
$ lein repl

user=> (dev)
15:30:32.412 [nREPL-worker-1] DEBUG mount - >> starting..  app-config
15:30:32.414 [nREPL-worker-1] INFO  app.config - loading config from test/resources/config.edn
15:30:32.422 [nREPL-worker-1] DEBUG mount - >> starting..  conn
15:30:32.430 [nREPL-worker-1] INFO  app.nyse - conf:  {:datomic {:uri datomic:mem://mount}, :h2 {:classname org.h2.Driver, :subprotocol h2, :subname jdbc:h2:mem:mount, :user sa, :password }, :rabbit {:api-port 15672, :password guest, :queue r-queue, :username guest, :port 5672, :node jabit, :exchange-type direct, :host 192.168.1.1, :vhost /captoman, :auto-delete-q? true, :routing-key , :exchange foo}}
15:30:32.430 [nREPL-worker-1] INFO  app.nyse - creating a connection to datomic: datomic:mem://mount
15:30:32.430 [nREPL-worker-1] DEBUG mount - >> starting..  nrepl
dev=>
```

everything is started and can be played with:

```clojure
dev=> (create-nyse-schema)
dev=> (add-order "GOOG" 665.51M 665.59M 100)
dev=> (add-order "GOOG" 665.50M 665.58M 300)

dev=> (find-orders "GOOG")
({:db/id 17592186045418, :order/symbol "GOOG", :order/bid 665.51M, :order/qty 100, :order/offer 665.59M}
 {:db/id 17592186045420, :order/symbol "GOOG", :order/bid 665.50M, :order/qty 300, :order/offer 665.58M})
```

once something is changed in the code, or you just need to reload everything, do `(reset)`:

```clojure
dev=> (reset)
15:32:44.342 [nREPL-worker-2] DEBUG mount - << stopping..  nrepl
15:32:44.343 [nREPL-worker-2] DEBUG mount - << stopping..  conn
15:32:44.343 [nREPL-worker-2] INFO  app.nyse - disconnecting from  datomic:mem://mount
15:32:44.344 [nREPL-worker-2] DEBUG mount - << stopping..  app-config

:reloading (app.config app.nyse app.utils.datomic app dev)
15:32:44.371 [nREPL-worker-2] DEBUG mount - >> starting..  app-config
15:32:44.372 [nREPL-worker-2] INFO  app.config - loading config from test/resources/config.edn
15:32:44.380 [nREPL-worker-2] DEBUG mount - >> starting..  conn
15:32:44.382 [nREPL-worker-2] INFO  app.nyse - conf:  {:datomic {:uri datomic:mem://mount}, :h2 {:classname org.h2.Driver, :subprotocol h2, :subname jdbc:h2:mem:mount, :user sa, :password }, :rabbit {:api-port 15672, :password guest, :queue r-queue, :username guest, :port 5672, :node jabit, :exchange-type direct, :host 192.168.1.1, :vhost /captoman, :auto-delete-q? true, :routing-key , :exchange foo}}
15:32:44.382 [nREPL-worker-2] INFO  app.nyse - creating a connection to datomic: datomic:mem://mount
15:32:44.387 [nREPL-worker-2] DEBUG mount - >> starting..  nrepl
:ready
```

notice that it stopped and started again.

In nyse's connection [:stop](https://github.com/tolitius/mount/blob/a63c725dcb6afd7ebb65f8a767d69ee0826921e8/test/app/nyse.clj#L18) 
function database is deleted. Hence after `(reset)` was called the app was brought its starting point: database was created by the
[:start](https://github.com/tolitius/mount/blob/a63c725dcb6afd7ebb65f8a767d69ee0826921e8/test/app/nyse.clj#L11) function, 
but no schema again:

```clojure
dev=> (find-orders "GOOG")

IllegalArgumentExceptionInfo :db.error/not-an-entity Unable to resolve entity: :order/symbol  datomic.error/arg (error.clj:57)
```

hence the app is in its "clean" state, and ready to rock and roll as right after the REPL started:

```clojure
dev=> (create-nyse-schema)
dev=> (find-orders "GOOG")
()

dev=> (add-order "AAPL" 111.712M 111.811M 250)

dev=> (find-orders "AAPL")
({:db/id 17592186045418, :order/symbol "AAPL", :order/bid 111.712M, :order/qty 250, :order/offer 111.811M})
```

## License

Copyright © 2015 tolitius

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
