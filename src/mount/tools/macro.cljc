(ns mount.tools.macro
  (:refer-clojure :exclude [case])
  #?(:cljs (:require-macros [mount.tools.macro :refer [deftime case]])))

;; From https://github.com/cgrand/macrovich 0.2.0
;; Licensed under EPL v1. Copyright Cristophe Grand.
(defmacro deftime
  "This block will only be evaluated at the correct time for macro definition, at other times its content
   are removed.
   For Clojure it always behaves like a `do` block.
   For Clojurescript/JVM the block is only visible to Clojure.
   For self-hosted Clojurescript the block is only visible when defining macros in the pseudo-namespace."
  [& body]
  (when #?(:clj (not (:ns &env)) :cljs (re-matches #".*\$macros" (name (ns-name *ns*))))
    `(do ~@body)))

(defmacro usetime
  "This block content is not included at macro definition time.
   For Clojure it always behaves like a `do` block.
   For Clojurescript/JVM the block is only visible to Clojurescript.
   For self-hosted Clojurescript the block is invisible when defining macros in the pseudo-namespace."
  [& body]
  (when #?(:clj true :cljs (not (re-matches #".*\$macros" (name (ns-name *ns*)))))
    `(do ~@body)))

(defmacro case [& {:keys [cljs clj]}]
  (if (contains? &env '&env)
    `(if (:ns ~'&env) ~cljs ~clj)
    (if #?(:clj (:ns &env) :cljs true)
      cljs
      clj)))

(deftime

(defmacro on-error [msg f & {:keys [fail?]
                             :or {fail? true}}]
  `(case
     :clj  (try ~f
             (catch Throwable t#
               (if ~fail?
                 (throw (RuntimeException. ~msg t#))
                 {:f-failed (ex-info ~msg {} t#)})))
     :cljs (try ~f
             (catch :default t#
               (if ~fail?
                 (throw (~'str ~msg " " t#))
                 {:f-failed (ex-info ~msg {} t#)})))))

(defmacro throw-runtime [msg]
  `(throw (case :clj  (RuntimeException. ~msg)
                :cljs (~'str ~msg))))

)

;; this is a one to one copy from https://github.com/clojure/tools.macro
;; to avoid a lib dependency for a single function

(defn name-with-attributes
  "To be used in macro definitions.
   Handles optional docstrings and attribute maps for a name to be defined
   in a list of macro arguments. If the first macro argument is a string,
   it is added as a docstring to name and removed from the macro argument
   list. If afterwards the first macro argument is a map, its entries are
   added to the name's metadata map and the map is removed from the
   macro argument list. The return value is a vector containing the name
   with its extended metadata map and the list of unprocessed macro
   arguments."
  [name macro-args]
  (let [[docstring macro-args] (if (string? (first macro-args))
                                 [(first macro-args) (next macro-args)]
                                 [nil macro-args])
    [attr macro-args]          (if (map? (first macro-args))
                                 [(first macro-args) (next macro-args)]
                                 [{} macro-args])
    attr                       (if docstring
                                 (assoc attr :doc docstring)
                                 attr)
    attr                       (if (meta name)
                                 (conj (meta name) attr)
                                 attr)]
    [(with-meta name attr) macro-args]))
