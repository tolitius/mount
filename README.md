# mount

Riding side by side with [tools.namespace](https://github.com/clojure/tools.namespace) to manage application state during development.

![Clojars Project](http://clojars.org/mount/latest-version.svg)

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

`mount` is here to preserve all the Clojure superpowers while making application state enjoyably reloadable.

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

[here](https://github.com/tolitius/mount/blob/master/test/mount/nyse.clj) 
is an example of a Datomic connection that "depends" on the similar `app-config`.

## The Importance of Being Reloadable

`mount` has start and stop functions that will walk all the states created with `defstate` and start / stop them
accordingly: i.e. will call their `:start` and `:stop` defined functions.

This can be easily hooked up to [tool.namespace](https://github.com/clojure/tools.namespace), to make the whole
application reloadable. Here is a [dev.clj](https://github.com/tolitius/mount/blob/master/dev/dev.clj) as 
an example.

## Mount and Develop!

`mount` comes with an example [app](https://github.com/tolitius/mount/blob/master/test/mount/app.clj) 
that has two states:

* `config`, loaded from the files and refreshed on each `(reset)`
* `datamic connection` that uses the config to create itself

### Running New York Stock Exchange

To try it out, clone `mount`, get to REPL and switch to `(dev)`:

```clojure
$ lein repl

user=> (dev)
20:37:29.461 [nREPL-worker-0] INFO  mount.config - loading config from test/resources/config.edn
20:37:29.477 [nREPL-worker-0] INFO  mount.nyse - creating a connection to datomic: datomic:mem://mount
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
20:38:43.244 [nREPL-worker-1] INFO  mount.nyse - disconnecting from  datomic:mem://mount
:reloading (mount mount.config mount.nyse mount.utils.datomic mount.app dev)

20:38:43.287 [nREPL-worker-1] INFO  mount.config - loading config from test/resources/config.edn
20:38:43.296 [nREPL-worker-1] INFO  mount.nyse - creating a connection to datomic: datomic:mem://mount
:ready
```

notice that it stopped and started again.

After the REPL was just started, schema was not created. Since the app was `(reset)`, it was brought to that starting point, so no schema again:

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

## license

Copyright © 2015 tolitius

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
