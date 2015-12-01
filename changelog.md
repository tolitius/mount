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
