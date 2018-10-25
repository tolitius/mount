## 0.1.14
###### Thu Oct 25 18:34:22 2018 -0400

* cljs: throw js/Error not just a string ([#100](https://github.com/tolitius/mount/issues/100))
* add ^{:on-lazy-start :throw} ([#95](https://github.com/tolitius/mount/issues/95) and [#99](https://github.com/tolitius/mount/issues/99))
* self hosted ClojureScript support ([#85](https://github.com/tolitius/mount/issues/85) and [#97](https://github.com/tolitius/mount/issues/97))

## 0.1.12
###### Sat Feb 10 14:53:04 2018 -0500

* remove ref to old state alias in `swap-states` ([#89](https://github.com/tolitius/mount/issues/89))
* `:on-reload :noop` should not leave stale references
* `stop-except` & `start-without` to take all states ([#81](https://github.com/tolitius/mount/issues/81))
* goog.log/error for logging cljs errors ([#63](https://github.com/tolitius/mount/issues/63))
* on failure (down) reports and cleans up vs. just "throw" ([#63](https://github.com/tolitius/mount/issues/63))
* do not start a `DerefableState` as a side-effect of printing ([#76](https://github.com/tolitius/mount/issues/76))

## 0.1.11
###### Fri Dec 2 18:09:51 2016 -0600

* opening `(find-all-states)`
* stop accepts a collection of states (in addition to varargs) ([#69](https://github.com/tolitius/mount/issues/69))
* swap-states and start-with-states take values ([#68](https://github.com/tolitius/mount/issues/68))
* adding `(running-states)` that returns a set
* `(mount/start #{})` is a noop ([#65](https://github.com/tolitius/mount/issues/65))
* removing log on state discovery
* adding a restart listener with watchers

## 0.1.10
###### Sun Feb 28 18:20:52 2016 -0500

* runtime args are now initialized to `{}` (rather than to `:no-args`)
* composing states on mount start ([#47](https://github.com/tolitius/mount/issues/47))
* removing `:suspend` and `:resume` ([#46](https://github.com/tolitius/mount/issues/46))

## 0.1.9
###### Sun Jan 31 15:47:19 2016 -0500

* `:on-reload` #{:noop :stop :restart} ([#36](https://github.com/tolitius/mount/issues/36))
* swapping states with values ([#45](https://github.com/tolitius/mount/issues/45))
* `(mount.core/system)` experiment is refactored to [Yurt](https://github.com/tolitius/yurt)
* cleaning up deleted states ([#42](https://github.com/tolitius/mount/issues/42))
* refactoring mount sample app (4 states, stateless fns)
* refactoring cljs logging to Closure (goog.log)

## 0.1.8
###### Mon Jan 4 14:09:17 2016 -0500

* pluging in [boot-check](https://github.com/tolitius/boot-check)
* refactoring reader conditionals out of cljs exceptions macro (thx [@DomKM](https://github.com/DomKM))
* riding on [boot-stripper](https://github.com/tolitius/boot-stripper)
* mount.tools.graph: [states-with deps](https://github.com/tolitius/mount/blob/0.1.8/src/mount/tools/graph.cljc#L20)
* fixing bug in start-with-args (thx [@malchmih](https://github.com/malchmih)) ([#30](https://github.com/tolitius/mount/issues/30))
* states with no :stop are restarted on ns recompile ([#22](https://github.com/tolitius/mount/issues/22)), ([#25](https://github.com/tolitius/mount/issues/25)), ([#26](https://github.com/tolitius/mount/issues/26))
* restarting a state on ns recompile ([#22](https://github.com/tolitius/mount/issues/22))

## 0.1.7
###### Mon Dec 21 20:52:31 2015 -0500

* making mount [boot](https://github.com/boot-clj/boot)'iful
* cljs `:classifier "aot"` is fixed by boot ([#23](https://github.com/tolitius/mount/issues/23))
* refactoring example app: + www
* stopping/cleaning state when its namespace is recompiled ([#22](https://github.com/tolitius/mount/issues/22))

## 0.1.6
###### Thu Dec 10 00:40:18 2015 -0500

* adding full ClojureScript support ([#10](https://github.com/tolitius/mount/issues/10))
* removing all the dependencies (`:dependencies []`)
* adding a sample [cljs app](https://github.com/tolitius/mount/blob/1ac28981a6a63a103a9057fd34a338c37acb913b/doc/clojurescript.md#mounting-that-clojurescript) (datascript, websockets)
* introducting `cljc` and `clj` [modes](https://github.com/tolitius/mount/blob/1ac28981a6a63a103a9057fd34a338c37acb913b/doc/clojurescript.md#mount-modes)
* `DerefableState`: states are _optionally_ derefable (via `IDeref`)
* removing dependency on var's meta

## 0.1.5
###### Tue Dec 1 08:58:26 2015 -0500

* cleaning up stale states ([#18](https://github.com/tolitius/mount/issues/18))
* adding ns to state order to avoid collisions
* consolidating status ([#19](https://github.com/tolitius/mount/issues/19))
* lifecycle fns take fns and values ([#20](https://github.com/tolitius/mount/issues/20))
* not retaining heads in side-effectful iterations ([#17](https://github.com/tolitius/mount/issues/17))
* logging AOP for REPL examples ([#15](https://github.com/tolitius/mount/issues/15))
* lifecycle functions return states touched ([#15](https://github.com/tolitius/mount/issues/15))
* removing tools.logging dep ([#15](https://github.com/tolitius/mount/issues/15))
* removing tools.macro dep
* removing tools.namespace dep

## 0.1.4
###### Sat Nov 21 15:31:13 2015 -0500

* [suspendable states](https://github.com/tolitius/mount#suspending-and-resuming)
* [stop-except](https://github.com/tolitius/mount#stop-an-application-except-certain-states)

## 0.1.3
###### Wed Nov 18 00:43:44 2015 -0500

* states-with-deps [[#12](https://github.com/tolitius/mount/issues/12)]
* mount => mount.core [[#11](https://github.com/tolitius/mount/issues/11)]
* states without `:stop` still become `NotStartedState` on `(mount/stop)`

## 0.1.2
###### Sun Nov 15 23:15:20 2015 -0500

* [swapping alternate implementations](https://github.com/tolitius/mount#swapping-alternate-implementations)
* [start-without](https://github.com/tolitius/mount#start-an-application-without-certain-states)

## 0.1.1
###### Sat Nov 14 16:40:38 2015 -0500

* [support for runtime arguments](https://github.com/tolitius/mount#runtime-arguments)

## 0.1.0
###### Fri Nov 13 17:00:43 2015 -0500

* defstate/start/stop
* welcome mount
