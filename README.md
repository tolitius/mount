> I think that it's _extraordinarily important_ that we in computer science keep fun in computing

_**Alan J. Perlis** from [Structure and Interpretation of Computer Programs](https://mitpress.mit.edu/sicp/full-text/book/book-Z-H-3.html)_

# mount

  module  |  branch  |  status
----------|----------|----------
   mount  | `master` | [![Circle CI](https://circleci.com/gh/tolitius/mount/tree/master.png?style=svg)](https://circleci.com/gh/tolitius/mount/tree/master)
   mount  | `0.1.8` | [![Circle CI](https://circleci.com/gh/tolitius/mount/tree/0.1.8.png?style=svg)](https://circleci.com/gh/tolitius/mount/tree/0.1.8)

[![Clojars Project](http://clojars.org/mount/latest-version.svg)](http://clojars.org/mount)

> <img src="doc/img/slack-icon.png" width="30px"> _any_ questions or feedback: [`#mount`](https://clojurians.slack.com/messages/mount/) clojurians slack channel (or just [open an issue](https://github.com/tolitius/mount/issues))

-

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [Why?](#why)
  - [Differences from Component](#differences-from-component)
- [How](#how)
  - [Creating State](#creating-state)
  - [Using State](#using-state)
- [Dependencies](#dependencies)
  - [Talking States](#talking-states)
- [Value of Values](#value-of-values)
- [The Importance of Being Reloadable](#the-importance-of-being-reloadable)
- [Start and Stop Order](#start-and-stop-order)
- [Start and Stop Parts of Application](#start-and-stop-parts-of-application)
- [Start an Application Without Certain States](#start-an-application-without-certain-states)
- [Stop an Application Except Certain States](#stop-an-application-except-certain-states)
- [Swapping Alternate Implementations](#swapping-alternate-implementations)
- [Suspending and Resuming](#suspending-and-resuming)
  - [Suspendable Lifecycle](#suspendable-lifecycle)
  - [Plugging into (reset)](#plugging-into-reset)
  - [Suspendable Example Application](#suspendable-example-application)
- [ClojureScript is Clojure](doc/clojurescript.md#managing-state-in-clojurescript)
- [Affected States](#affected-states)
- [Recompiling Namespaces with Running States](#recompiling-namespaces-with-running-states)
- [Logging](#logging)
- [Mount and Develop!](#mount-and-develop)
  - [Running New York Stock Exchange](#running-new-york-stock-exchange)
  - [New York Stock Exchange Maintenance](#new-york-stock-exchange-maintenance)
- [Web and Uberjar](#web-and-uberjar)
- [Runtime Arguments](#runtime-arguments)
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

### Differences from Component

mount is an alternative to the [component](https://github.com/stuartsierra/component) approach with notable [differences](doc/differences-from-component.md#differences-from-component).

## How

```clojure
(require '[mount.core :refer [defstate]])
```

### Creating State

Creating state is easy:

```clojure
(defstate conn :start create-conn)
```

where the `create-conn` function is defined elsewhere, can be right above it.

In case this state needs to be cleaned / destroyed between reloads, there is also `:stop`

```clojure
(defstate conn :start create-conn
               :stop (disconnect conn))
```

That is pretty much it. But wait, there is more.. this state is _a top level being_, which means it can be simply
`required` by other namespaces or in REPL:

```clojure
dev=> (require '[app.nyse :refer [conn]])
nil
dev=> conn
#object[datomic.peer.LocalConnection 0x1661a4eb "datomic.peer.LocalConnection@1661a4eb"]
```

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
  (:require [mount.core :refer [defstate]]))

(defstate app-config
  :start (load-config "test/resources/config.edn"))
```

this `app-config`, being top level, can be used in other namespaces, including the ones that create states:

```clojure
(ns app.database
  (:require [mount.core :refer [defstate]]
            [app.config :refer [app-config]]))

(defstate conn :start (create-connection app-config))
```

[here](dev/clj/app/nyse.clj)
is an example of a Datomic connection that "depends" on a similar `app-config`.

## Value of values

Lifecycle functions start/stop/suspend/resume can take both functions and values. This is "valuable" and also works:

```clojure
(defstate answer-to-the-ultimate-question-of-life-the-universe-and-everything :start 42)
```

Besides scalar values, lifecycle functions can take anonymous functions, partial functions, function references, etc.. Here are some examples: 

```clojure
(defn f [n]
  (fn [m]
    (+ n m)))

(defn g [a b]
  (+ a b))

(defn- pf [n]
  (+ 41 n))

(defn fna []
  42)

(defstate scalar :start 42)
(defstate fun :start #(inc 41))
(defstate with-fun :start (inc 41))
(defstate with-partial :start (partial g 41))
(defstate f-in-f :start (f 41))
(defstate f-no-args-value :start (fna))
(defstate f-no-args :start fna)
(defstate f-args :start g)
(defstate f-value :start (g 41 1))
(defstate private-f :start pf)
```

Check out [fun-with-values-test](test/mount/test/fun_with_values.cljc) for more details.

## The Importance of Being Reloadable

`mount` has start and stop functions that will walk all the states created with `defstate` and start / stop them
accordingly: i.e. will call their `:start` and `:stop` defined functions. Hence the whole applicatoin state can be reloaded in REPL e.g.:

```
dev=> (require '[mount.core :as mount])

dev=> (mount/stop)
dev=> (mount/start)
```

While it is not always necessary, mount lificycle can be easily hooked up to [tools.namespace](https://github.com/clojure/tools.namespace), 
to make the whole application reloadable with refreshing the app namespaces. 
Here is a [dev.clj](dev/clj/dev.clj) as an example, that sums up to:

```clojure
(defn go []
  (start)
  :ready)

(defn reset []
  (stop)
  (tn/refresh :after 'dev/go))
```

the `(reset)` is then used in REPL to restart / reload application state without the need to restart the REPL itself.

## Start and Stop Order

Since dependencies are "injected" by `require`ing on the namespace level, `mount` **trusts the Clojure compiler** to
maintain the start and stop order for all the `defstates`.

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

You can see examples of start and stop flows in the [example app](README.md#mount-and-develop).

## Start and Stop Parts of Application

In REPL or during testing it is often very useful to work with / start / stop _only a part_ of an application, i.e. "only these two states".

`mount` start/stop functions _optionally_ take namespaces to start/stop:

```clojure
(mount/start #'app.config/app-config #'app.nyse/conn)
...
(mount/stop #'app.config/app-config #'app.nyse/conn)
```

which will only start/stop `app-config` and `conn` (won't start any other states).

Here is an [example](test/mount/test/parts.cljc) test that uses only two namespaces checking that the third one is not started.

## Start an Application Without Certain States

Whether it is in REPL or during testing, it is often useful to start an application _without_ certain states. These can be queue listeners that are not needed at REPL time, or a subset of an application to test.

The `start-without` function can do just that:

```clojure
(mount/start-without #'app.feeds/feed-listener 
                     #'app/nrepl)
```

which will start an application without starting `feed-listener` and `nrepl` states.

Here is an [example](test/mount/test/start_without.cljc) test that excludes Datomic connection and nREPL from an application on start.

## Swapping Alternate Implementations

During testing it is often very useful to mock/stub certain states. For example runnig a test against an in memory database vs. the real one, running with a publisher that publishes to a test core.async channel vs. the real remote queue, etc.

The `start-with` function can do just that:

```clojure
(mount/start-with {#'app.nyse/db        #'app.test/test-db
                   #'app.nyse/publisher #'app.test/test-publisher})
```

`start-with` takes a map of states with their substitutes. For example `#'app.nyse/db` here is the real deal (remote) DB that is being substituted with `#'app.test/test-db` state, which could be anything, a map, an in memory DB, etc.

One thing to note, whenever

```clojure
(mount/stop)
```

is run after `start-with`, it rolls back to an original "state of states", i.e. `#'app.nyse/db` is `#'app.nyse/db` again. So subsequent calls to `(mount/start)` or even to `(mount/start-with {something else})` will start from a clean slate.

Here is an [example](test/mount/test/start_with.cljc) test that starts an app with mocking Datomic connection and nREPL.

## Stop an Application Except Certain States

Calling `(mount/stop)` will stop all the application states. In case everything needs to be stopped _besides certain ones_, it can be done with `(mount/stop-except)`.

Here is an example of restarting the application without bringing down `#'app.www/nyse-app`: 

```clojure
dev=> (mount/start)
14:34:10.813 [nREPL-worker-0] INFO  mount.core - >> starting..  app-config
14:34:10.814 [nREPL-worker-0] INFO  mount.core - >> starting..  conn
14:34:10.814 [nREPL-worker-0] INFO  app.db - creating a connection to datomic: datomic:mem://mount
14:34:10.838 [nREPL-worker-0] INFO  mount.core - >> starting..  nyse-app
14:34:10.843 [nREPL-worker-0] DEBUG o.e.j.u.component.AbstractLifeCycle - STARTED SelectChannelConnector@0.0.0.0:4242
14:34:10.843 [nREPL-worker-0] DEBUG o.e.j.u.component.AbstractLifeCycle - STARTED org.eclipse.jetty.server.Server@194f37af
14:34:10.844 [nREPL-worker-0] INFO  mount.core - >> starting..  nrepl
:started

dev=> (mount/stop-except #'app.www/nyse-app)
14:34:47.766 [nREPL-worker-0] INFO  mount.core - << stopping..  nrepl
14:34:47.766 [nREPL-worker-0] INFO  mount.core - << stopping..  conn
14:34:47.766 [nREPL-worker-0] INFO  app.db - disconnecting from  datomic:mem://mount
14:34:47.766 [nREPL-worker-0] INFO  mount.core - << stopping..  app-config
:stopped
dev=>

dev=> (mount/start)
14:34:58.673 [nREPL-worker-0] INFO  mount.core - >> starting..  app-config
14:34:58.674 [nREPL-worker-0] INFO  app.config - loading config from test/resources/config.edn
14:34:58.674 [nREPL-worker-0] INFO  mount.core - >> starting..  conn
14:34:58.674 [nREPL-worker-0] INFO  app.db - creating a connection to datomic: datomic:mem://mount
14:34:58.693 [nREPL-worker-0] INFO  mount.core - >> starting..  nrepl
:started
```

Notice that the `nyse-app` is not started the second time (hence no more accidental `java.net.BindException: Address already in use`). It is already up and running.

## Suspending and Resuming

Besides starting and stopping states can also be suspended and resumed. While this is not needed most of the time, it does comes really handy _when_ this need is there. For example:

* while working in REPL, you only want to truly restart a web server/queue listener/db connection _iff_ something changed, all other times `(mount/stop)` / `(mount/start)` or `(reset)` is called, these states should not be restarted. This might have to do with time to connect / bound ports / connection timeouts, etc..

* when taking an application out of rotation in a data center, and then phasing it back in, it might be handy to still keep it _up_, but suspend all the client / novelty facing components in between. 

and some other use cases.

### Suspendable Lifecycle

In additiong to `start` / `stop` functions, a state can also have `resume` and, if needed, `suspend` ones:

```clojure
(defstate web-server :start start-server
                     :resume resume-server
                     :stop stop-server)

```

`suspend` function is optional. Combining this with [(mount/stop-except)](#stop-an-application-except-certain-states), can result in an interesting restart behavior where everything is restared, but this `web-server` is _resumed_ instead (in this case `#'app.www/nyse-app` is an example of the above `web-server`):

```clojure
dev=> (mount/stop-except #'app.www/nyse-app)
14:44:33.991 [nREPL-worker-1] INFO  mount.core - << stopping..  nrepl
14:44:33.992 [nREPL-worker-1] INFO  mount.core - << stopping..  conn
14:44:33.992 [nREPL-worker-1] INFO  app.db - disconnecting from  datomic:mem://mount
14:44:33.992 [nREPL-worker-1] INFO  mount.core - << stopping..  app-config
:stopped
dev=>

dev=> (mount/suspend)
14:44:52.467 [nREPL-worker-1] INFO  mount.core - >> suspending..  nyse-app
:suspended
dev=>

dev=> (mount/start)
14:45:00.297 [nREPL-worker-1] INFO  mount.core - >> starting..  app-config
14:45:00.297 [nREPL-worker-1] INFO  mount.core - >> starting..  conn
14:45:00.298 [nREPL-worker-1] INFO  app.db - creating a connection to datomic: datomic:mem://mount
14:45:00.315 [nREPL-worker-1] INFO  mount.core - >> resuming..  nyse-app
14:45:00.316 [nREPL-worker-1] INFO  mount.core - >> starting..  nrepl
:started
```

Notice `>> resuming..  nyse-app`, which in [this case](https://github.com/tolitius/mount/blob/suspendable/test/app/www.clj#L32) just recreates Datomic schema vs. doing that _and_ starting the actual web server.

### Plugging into (reset)

In case `tools.namespace` is used, this lifecycle can be easily hooked up with `dev.clj`:

```clojure
(defn start []
  (mount/start))

(defn stop []
  (mount/suspend)
  (mount/stop-except #'app.www/nyse-app))

(defn reset []
  (stop)
  (tn/refresh :after 'dev/start))
```

### Suspendable Example Application

An [example application](https://github.com/tolitius/mount/tree/suspendable/test/app) with a suspendable web server and `dev.clj` lives in the `suspendable` branch. You can clone mount and try it out:

```
$ git checkout suspendable
Switched to branch 'suspendable'
```

## Recompiling Namespaces with Running States

Mount will detect when a namespace with states (i.e. with `(defstate ...)`) was reloaded/recompiled, 
and will check every state in this namespace whether it was running at the point of recompilation. If it was, _it will restart it_:

* if a state has a `:stop` function, mount will invoke it on the old version of state (i.e. cleanup)
* it will call a "new" `:start` function _after_ this state is recompiled/redefined

Mount won't keep it a secret, it'll tell you about all the states that had to be restarted during namespace reload/recompilation:

<img src="doc/img/ns-recompile.png" width="500px">

Providing a `:stop` function _is_ optional, but in case a state needs to be cleaned between restarts or on a system shutdown,
`:stop` is highly recommended.

## Affected States

Every time a lifecycle function (start/stop/suspend/resume) is called mount will return all the states that were affected:

```clojure
dev=> (mount/start)
{:started [#'app.config/app-config 
           #'app.nyse/conn 
           #'app/nrepl
           #'check.suspend-resume-test/web-server
           #'check.suspend-resume-test/q-listener]}
```
```clojure
dev=> (mount/suspend)
{:suspended [#'check.suspend-resume-test/web-server
             #'check.suspend-resume-test/q-listener]}
```
```clojure
dev=> (mount/start)
{:started [#'check.suspend-resume-test/web-server
           #'check.suspend-resume-test/q-listener]}
```

An interesting bit here is a vector vs. a set: all the states are returned _in the order they were changed_.

## Logging

> All the mount examples have `>> starting..` / `<< stopping..` logging messages, but when I develop an application with mount I don't see them.

Valid question. It was a [conscious choice](https://github.com/tolitius/mount/issues/15) not to depend on any particular logging library, since there are few to select from, and this decision is best left to the developer who may choose to use mount. 

Since mount is a _library_ it should _not_ bring any dependencies unless its functionality directly depends on them.

> But I still these logging statements in the examples.

The way this is done is via an excellent [robert hooke](https://github.com/technomancy/robert-hooke/). Example applications live in `test`, so does the [utility](https://github.com/tolitius/mount/blob/75d7cdc610ce38623d4d3aea1da3170d1c9a3b4b/test/app/utils/logging.clj#L44) that adds logging to all the mount's lifecycle functions on start in [dev.clj](https://github.com/tolitius/mount/blob/75d7cdc610ce38623d4d3aea1da3170d1c9a3b4b/dev/dev.clj#L21).

## Mount and Develop!

`mount` comes with an example [app](dev/clj/app)
that has 3 states:

* `config`, loaded from the files and refreshed on each `(reset)`
* `datomic connection` that uses the config to create itself
* `nrepl` that uses config to bind to host/port

### Running New York Stock Exchange

To try it out, clone `mount`, get to REPL (`boot repl` or `lein repl`) and switch to `(dev)`:

```clojure
$ boot repl

user=> (dev)
#object[clojure.lang.Namespace 0xcf1a0cc "dev"]
```

start/restart/reset everything using `(reset)`:

```clojure
dev=> (reset)

:reloading (mount.tools.macro mount.core app.utils.logging app.conf app.db app.utils.datomic app.nyse app.www app.example dev)
INFO  app.utils.logging - >> starting..  #'app.conf/config
INFO  app.conf - loading config from dev/resources/config.edn
INFO  app.utils.logging - >> starting..  #'app.db/conn
INFO  app.db - conf:  {:datomic {:uri datomic:mem://mount}, :www {:port 4242}, :h2 {:classname org.h2.Driver, :subprotocol h2, :subname jdbc:h2:mem:mount, :user sa, :password }, :rabbit {:api-port 15672, :password guest, :queue r-queue, :username guest, :port 5672, :node jabit, :exchange-type direct, :host 192.168.1.1, :vhost /captoman, :auto-delete-q? true, :routing-key , :exchange foo}, :nrepl {:host 0.0.0.0, :port 7878}}
INFO  app.db - creating a connection to datomic: datomic:mem://mount
INFO  app.utils.logging - >> starting..  #'app.www/nyse-app
INFO  app.utils.logging - >> starting..  #'app.example/nrepl
dev=>
```

everything is started and can be played with:

```clojure
dev=> (add-order "GOOG" 665.51M 665.59M 100)
dev=> (add-order "GOOG" 665.50M 665.58M 300)

dev=> (find-orders "GOOG")
({:db/id 17592186045418, :order/symbol "GOOG", :order/bid 665.51M, :order/qty 100, :order/offer 665.59M}
 {:db/id 17592186045420, :order/symbol "GOOG", :order/bid 665.50M, :order/qty 300, :order/offer 665.58M})
```

since there is also a web server running, we can add orders with HTTP POST (from a different terminal window):

```clojure
$ curl -X POST -d "ticker=TSLA&qty=100&bid=232.38&offer=232.43" "http://localhost:4242/nyse/orders"

{"added":{"ticker":"TSLA","qty":"100","bid":"232.38","offer":"232.43"}}
```

```clojure
dev=> (find-orders "TSLA")
({:db/id 17592186045422, :order/symbol "TSLA", :order/bid 232.38M, :order/qty 100, :order/offer 232.43M})
```

once something is changed in the code, or you just need to reload everything, do `(reset)`:

```clojure
dev=> (reset)
INFO  app.utils.logging - << stopping..  #'app.example/nrepl
INFO  app.utils.logging - << stopping..  #'app.www/nyse-app
INFO  app.utils.logging - << stopping..  #'app.db/conn
INFO  app.db - disconnecting from  datomic:mem://mount
INFO  app.utils.logging - << stopping..  #'app.conf/config

:reloading (app.conf app.db app.nyse app.www app.example dev)

INFO  app.utils.logging - >> starting..  #'app.conf/config
INFO  app.conf - loading config from dev/resources/config.edn
INFO  app.utils.logging - >> starting..  #'app.db/conn
INFO  app.db - conf:  {:datomic {:uri datomic:mem://mount}, :www {:port 4242}, :h2 {:classname org.h2.Driver, :subprotocol h2, :subname jdbc:h2:mem:mount, :user sa, :password }, :rabbit {:api-port 15672, :password guest, :queue r-queue, :username guest, :port 5672, :node jabit, :exchange-type direct, :host 192.168.1.1, :vhost /captoman, :auto-delete-q? true, :routing-key , :exchange foo}, :nrepl {:host 0.0.0.0, :port 7878}}
INFO  app.db - creating a connection to datomic: datomic:mem://mount
INFO  app.utils.logging - >> starting..  #'app.www/nyse-app
INFO  app.utils.logging - >> starting..  #'app.example/nrepl
:ready
```

notice that it stopped and started again.

In `app.db` connection `:stop` calls a `disconnect` function where a [database is deleted](https://github.com/tolitius/mount/blob/58ca345896e572d7b3fbe8fec21525428f846dd5/dev/clj/app/db.clj#L18). Hence after `(reset)` was called the app was brought its starting point: [database was created](https://github.com/tolitius/mount/blob/58ca345896e572d7b3fbe8fec21525428f846dd5/dev/clj/app/db.clj#L11) by the
`:start` that calls a `new-connection` function, and db schema is [created](https://github.com/tolitius/mount/blob/58ca345896e572d7b3fbe8fec21525428f846dd5/dev/clj/app/www.clj#L24) by `nyse.app`.

But again no orders:

```clojure
dev=> (find-orders "GOOG")
()
dev=> (find-orders "AAPL")
()
```

hence the app is in its "clean" state, and ready to rock and roll as right after the REPL started:

```clojure
dev=> (add-order "TSLA" 232.381M 232.436M 250)

dev=> (find-orders "TSLA")
({:db/id 17592186045420, :order/symbol "TSLA", :order/bid 232.381M, :order/qty 250, :order/offer 232.436M})
```

### New York Stock Exchange Maintenance

Say we want to leave the exchage functioning, but would like to make sure that noone can hit it from the web. Easy, just stop the web server:

```clojure
dev=> (mount/stop #'app.www/nyse-app)
INFO  app.utils.logging - << stopping..  #'app.www/nyse-app
{:stopped ["#'app.www/nyse-app"]}
dev=>
```
```bash
$ curl localhost:4242
curl: (7) Failed to connect to localhost port 4242: Connection refused
```

everything but the web server works as before:

```clojure
dev=> (find-orders "TSLA")
({:db/id 17592186045420, :order/symbol "TSLA", :order/bid 232.381M, :order/qty 250, :order/offer 232.436M})
dev=>
```

once we found who `DDoS`ed us on :4242, and punished them, we can restart the web server:

```clojure
dev=> (mount/start #'app.www/nyse-app)
INFO  app.utils.logging - >> starting..  #'app.www/nyse-app
{:started ["#'app.www/nyse-app"]}
dev=>
```

```clojure
$ curl localhost:4242
welcome to the mount sample app!
```

## Web and Uberjar

There is an `uberjar` branch with an example webapp and it's uberjar sibling. Before trying it:

```clojure
$ git checkout uberjar
Switched to branch 'uberjar'
```

The documentation is [here](doc/uberjar.md#creating-reloadable-uberjarable-app).

## Runtime Arguments

There is an `with-args` branch with an example app that takes command line params

```clojure
$ git checkout with-args
Switched to branch 'with-args'
```

The documentation is [here](doc/runtime-arguments.md#passing-runtime-arguments).

## License

Copyright © 2015 tolitius

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
