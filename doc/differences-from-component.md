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
  - [Objects vs. Namespaces](#objects-vs-namespaces)
  - [Start and Stop Order](#start-and-stop-order)
  - [Component requires whole app buy in](#component-requires-whole-app-buy-in)
  - [Refactoring an existing application](#refactoring-an-existing-application)
  - [Code navigation (vi, emacs, IDE..)](#code-navigation-vi-emacs-ide)
  - [Starting and stopping _parts_ of an application](#starting-and-stopping-_parts_-of-an-application)
  - [Boilerplate code](#boilerplate-code)
- [What Component does better](#what-component-does-better)
  - [Swapping alternate implementations](#swapping-alternate-implementations)
  - [Uberjar / Packaging](#uberjar--packaging)
  - [Multiple separate systems](#multiple-separate-systems)
  - [Visualizing dependency graph](#visualizing-dependency-graph)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Then why "mount"!?

[mount](https://github.com/tolitius/mount) was created after using Component for several projects. 

While Component is an interesting way to manage state, it has its limitations that prevented us
from having the ultimate super power of Clojure: _fun working with it_. Plus several other disadvantages 
that we wanted to "fix".

## So what are the differences?

### Objects vs. Namespaces

One thing that feels a bit "unClojure" about Component is "Objects". Objects everywhere, and Objects for everything.
This is how Component "separates explicit dependencies" and "clears the bounaries". 

This is also how an Object Oriented language does it, which does not leave a lot of room for functions: 
with Component most of the functions are _methods_ which is an important distinction.

Mount relies on Clojure namespaces to clear the boundaries. No change from Clojure here: `defstate` in one namespace
can be easily `:require`d in another.

### Start and Stop Order

Component relies on a cool [dependency](https://github.com/stuartsierra/dependency) library to build 
a graph of dependencies, and start/stop them via topological sort based on the dependencies in this graph.

Since Mount relies on Clojure namespaces and `:require`/`:use`, the order of states 
and their dependencies are revealed by the Clojure Compiler itself. Mount just records that order and replays 
it back and forth on stop and start.

### Component requires whole app buy in

Component really only works if you build your entire app around its model: application is fully based on Components 
where every Component is an Object.

Mount does not require you to "buy anything at all", it is free :) Just create a `defstate` whenever/whereever 
you need it and use it.

This one was a big deal for all the projects we used Component with, "the whole app buy in" converts an "_open_" application 
of Namespaces and Functions to a "_closed_" application of Objects and Methods. "open" and "close" 
here are rather feelings, but it is way easier and more natural to 

* go to a namespace to see this function
than to
* go to a namespace, go to a component, go to another component that this function maybe using/referenced at via a component key, to get the full view of the function.

Again this is mostly a personal preference: the code works in both cases.

### Refactoring an existing application

Since to get the most benefits of Component the approach is "all or nothing", to rewrite an existing application
in Component, depending on the application size, is daunting at best.

Mount allows adding `defstates` _incrementally_, the same way you would add functions to an application.

### Code navigation (vi, emacs, IDE..)

Navigation between functions in Component can't really be done without Components themselves. Since in Component
a function usually references another function via a map lookup: `(:function component)`. This is not a big deal, but
it changes the way IDE / editors are used to navigate the code by adding that extra step.

Since Mount relies on Clojure namespaces and `:require`/`:use`, the navigation accorss functions / states is exactly
the same with or without Mount: there are no extra click/mental steps.

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
(defstate name :start (fn) 
               :stop (fn))
```

no "ceremony".

## What Component does better

### Swapping alternate implementations

This is someting that is very useful for testing and is very easy to do in Component by simply assoc'ing onto a map.
In Mount you can redef the state, but it is not as elegant and decoupled as it is in Component.

###### _conclusion: needs more thinking._

### Uberjar / Packaging

Since Component fully controls the `system` where the whole application lives, it is quite simple 
to start an application from anywhere including a `-main` function of the uberjar.

In order to start the whole system in development, Mount just needs `(mount/start)` or `(reset)` 
it's [simple](https://github.com/tolitius/mount#the-importance-of-being-reloadable).

However there is no "tools.namespaces"/REPL at a "stand alone jar runtime" and in order for Mount to start / stop
the app, states need to be `:require`/`:use`d, which is usually done within the same namespace as `-main`. 

Depending on app dependencies, it could only require a few states to be `:require`/`:use`d, others 
will be brought transitively. Here is an [example](uberjar.md#creating-reloadable-uberjarable-app) of building a wepapp uberjar with Mount.

###### _conclusion: it's simple in Mount as well, but requires an additional step._

### Multiple separate systems

With Component multiple separate systems can be started in the same Clojure runtime with different settings. Which is very useful for testing.

Mount keeps states in namespaces, hence the app becomes "[The One](https://en.wikipedia.org/wiki/Neo_(The_Matrix))", and there can't be "multiples The Ones".

What Mount has going for it for testing is [starting / stopping parts of an application](https://github.com/tolitius/mount/blob/master/doc/differences-from-component.md#starting-and-stopping-parts-of-an-application) where only the part of the system that is being tested can be started.

###### _conclusion: needs more thinking._

### Visualizing dependency graph

Component keeps an actual graph which can be visualized with great libraries like [loom](https://github.com/aysylu/loom).
Having this visualization is really helpful, especially during code discusions between multiple developers.

Mount does not have this at the moment. It does have all the data to create such a visualization, perhaps even 
by building a graph out of the data it has just for this purpose.
