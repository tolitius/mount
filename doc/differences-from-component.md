###### Differences from "Component"

## Perception

Solving the "application state" in Clojure, where an application is not a tool or a library,
but a product that has lots of state to deal with, is not a trivial task.
The [Component](https://github.com/stuartsierra/component) framework is a solution that has been gaining popularity:

> _[source](http://www.javacodegeeks.com/2015/09/clojure-web-development-state-of-the-art.html):_

> _I think all agreed that Component is the industry standard for managing lifecycle of Clojure applications. If you are a Java developer you may think of it as a Spring (DI) replacement – you declare dependencies between “components” which are resolved on “system” startup. So you just say “my component needs a repository/database pool” and component library “injects” it for you._

While this is a common understanding, the Component is far from being Spring, in a good sense:

* its codebase is fairly small
* it aims to solve one thing and one thing only: manage application state via inversion of control

The not so hidden benefit is REPL time reloadability that it brings to the table with `component/start` and `component/stop`
<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [Then why "mount"!?](#then-why-mount)
- [So what are the differences?](#so-what-are-the-differences)
  - [Component requires whole app buy in](#component-requires-whole-app-buy-in)
  - [Start and Stop Order](#start-and-stop-order)
  - [Refactoring an existing application](#refactoring-an-existing-application)
  - [Code navigation](#code-navigation)
  - [Objects vs. Namespaces](#objects-vs-namespaces)
  - [Starting and stopping parts of an application](#starting-and-stopping-parts-of-an-application)
  - [Boilerplate code](#boilerplate-code)
  - [Library vs. Framework](#library-vs-framework)
- [What Component does better](#what-component-does-better)
  - [Multiple separate systems within the same JVM](#multiple-separate-systems-within-the-same-jvm)
  - [Visualizing dependency graph](#visualizing-dependency-graph)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Then why "mount"!?

[mount](https://github.com/tolitius/mount) was created after using Component for several projects. 

While Component is an interesting way to manage state, it has its limitations that prevented us
from having the ultimate super power of Clojure: _fun working with it_. Plus several other disadvantages 
that we wanted to "fix".

Before moving on to differences, [here](https://news.ycombinator.com/item?id=2467809) is a piece by Rich Hickey. While he is _not_ talking about application state, it is an interesting insight into LISP design principles:

> Lisps were designed to receive a set of interactions/forms via a REPL, not to compile files/modules/programs etc. This means you can build up a Lisp program interactively in very small pieces, switching between namespaces as you go, etc. It is a very valuable part of the Lisp programming experience. It implies that you can stream fragments of Lisp programs as small as a single form over sockets, and have them be compiled and evaluated as they arrive. It implies that you can define a macro and immediately have the compiler incorporate it in the compilation of the next form, or evaluate some small section of an otherwise broken file.

## So what are the differences?

### Component requires whole app buy in

Component really only works if you build your entire app around its model: application is fully based on Components 
where every Component is an Object.

Mount does not require you to "buy anything at all", it is free :) Just create a `defstate` whenever/wherever 
you need it and use it.

This one was a big deal for all the projects we used Component with, "the whole app buy in" converts an "_open_" application 
of Namespaces and Functions to a "_closed_" application of Objects and Methods. "open" and "close" 
here are rather feelings, but it is way easier and more natural to 

* go to a namespace to see this function
than to
* go to a namespace, go to a component, go to another component that this function maybe using/referenced at via a component key, to get the full view of the function.

Again this is mostly a personal preference: the code works in both cases.

### Start and Stop Order

Component relies on a cool [dependency](https://github.com/stuartsierra/dependency) library to build 
a graph of dependencies, and start/stop them via topological sort based on the dependencies in this graph.

Since Mount relies on Clojure namespaces and `:require`/`:use`, the order of states 
and their dependencies are revealed by the Clojure Compiler itself. Mount just records that order and replays 
it back and forth on stop and start.

### Refactoring an existing application

Since to get the most benefits of Component the approach is "all or nothing", to rewrite an existing application
in Component, depending on the application size, is daunting at best.

Mount allows adding `defstates` _incrementally_, the same way you would add functions to an application.

### Code navigation

Component changes the way the code is structured. Depending on the size of the code base, and how rich the dependency graph is, Component might add a good amount of cognitive load. To a simple navigation from namespace to namespace, from function to function, Components add, well.. "Components" that can't be ignored when [loading the codebase in one's head](http://paulgraham.com/head.html)

Since Mount relies on Clojure namespaces (`:require`/`:use`), navigation across functions / states is exactly
the same with or without Mount: there are no extra mental steps.

### Objects vs. Namespaces

One thing that feels a bit "unClojure" about Component is "Objects". Objects everywhere, and Objects for everything.
This is how Component "separates explicit dependencies" and "clears the bounaries". 

This is also how an Object Oriented language does it, which does not leave a lot of room for functions: 
with Component most of the functions are _methods_ which is an important distinction.

Mount relies on Clojure namespaces to clear the boundaries. No change from Clojure here: `defstate` in one namespace
can be easily `:require`d in another.

### Starting and stopping _parts_ of an application

Component can't really start and stop parts of an application within the same "system". Other sub systems can be 
created from scratch or by dissoc'ing / merging with existing systems, but it is usually not all 
that flexible in terms of REPL sessions where lots of time is spent.

Mount _can_ start and stop parts of an application via given states with their namespaces:

```clojure
dev=> (mount/start #'app.config/app-config #'app.nyse/conn)

11:35:06.753 [nREPL-worker-1] INFO  mount - >> starting..  app-config
11:35:06.756 [nREPL-worker-1] INFO  mount - >> starting..  conn
:started
dev=>
```

Here is more [documentation](../README.md#start-and-stop-parts-of-application) on how to start/stop parts of an app.

### Boilerplate code

Component does not require a whole lot of "extra" code but: 

* a system with dependencies
* components as records 
* with optional constructors 
* and a Lifecycle/start Lifecycle/stop implementations
* destructuring component maps

Depending on the number of application components the "extra" size may vary. 

Mount is pretty much:

```clojure
(defstate name :start fn 
               :stop fn)
```

no "ceremony".

### Library vs. Framework

Mount uses namespaces and vars where Component uses records and protocols.

Component manages protocols and records, and in order to do that it requires a whole app buyin, which makes it a _framework_. 

Mount does not need to manage namespaces and vars, since it is very well managed by the Clojure Compiler, which makes it a _library_.

## What Component does better

### Multiple separate systems within the same JVM

With Component multiple separate systems can be started _in the same Clojure runtime_ with different settings. Which _might_ be useful for testing, i.e. if you need to have `dev db` and `test db` started in the _same_ REPL, to _run tests within the same REPL you develop in_.

Development workflows vary and tend to be a subjective / preference based more than a true recipe, but I believe it is much cleaner to run tests in the _separate_ REPL / process. Moreover run them continuesly: i.e. `boot watch speak test`: this way you don't event need to look at that other REPL / terminal, Boot will _tell_ you whether the tests pass or fail after any file is changed.

Mount keeps states in namespaces, hence the app becomes "[The One](https://en.wikipedia.org/wiki/Neo_(The_Matrix))", and there can't be "multiples The Ones". In practice, if we are talking about stateful external resources, there is trully only _one_ of them with a given configuration. Different configuration => different state. It's is that simple.

Testing is not alien to Mount and it knows how to do a thing or two:

* [starting / stopping parts of an application](https://github.com/tolitius/mount/blob/master/doc/differences-from-component.md#starting-and-stopping-parts-of-an-application)
* [start an application without certain states](https://github.com/tolitius/mount#start-an-application-without-certain-states)
* [swapping alternate implementations](https://github.com/tolitius/mount#swapping-alternate-implementations)
* [stop an application except certain states](https://github.com/tolitius/mount#stop-an-application-except-certain-states)
* [composing states](https://github.com/tolitius/mount#composing-states)

After [booting mount](http://www.dotkam.com/2015/12/22/the-story-of-booting-mount/) I was secretly thinking of achieving multiple separate systems by running them in different [Boot Pods](https://github.com/boot-clj/boot/wiki/Pods).

But the more I think about it, the less it feels like a mount's core functionality. So I created [Yurt](https://github.com/tolitius/yurt) that can easily create and run multiple separate mount systems simultaniously.

###### _conclusion: can be done with mount as well, but via a different dependency._

### Visualizing dependency graph

Component keeps an actual graph which can be visualized with great libraries like [loom](https://github.com/aysylu/loom).
Having this visualization is really helpful, especially during code discusions between multiple developers.

Mount does not have this at the moment. It does have all the data to create such a visualization, perhaps even 
by building a graph out of the data it has just for this purpose.

There is a [`(states-with-deps)`](https://github.com/tolitius/mount/blob/master/src/mount/tools/graph.cljc#L20) function that can help out:

```clojure
dev=> (require '[mount.tools.graph :as graph])

dev=> (graph/states-with-deps)
({:name "#'app.conf/config", 
  :order 1, 
  :status #{:started}, 
  :deps #{}}
 {:name "#'app.db/conn",
  :order 2,
  :status #{:started},
  :deps #{"#'app.conf/config"}}
 {:name "#'app.www/nyse-app",
  :order 3,
  :status #{:started},
  :deps #{"#'app.conf/config"}}
 {:name "#'app.example/nrepl",
  :order 4,
  :status #{:started},
  :deps #{"#'app.www/nyse-app" "#'app.conf/config"}})
```

But it does not draw :)

###### _conclusion: needs more thinking._
