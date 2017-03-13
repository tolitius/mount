(ns mount.tools.macro
  #?(:cljs (:require-macros [mount.tools.macro])))

#?(:clj
    (defmacro if-clj [then else]
      (if (-> &env :ns not)
        then
        else)))

#?(:clj
    (defmacro on-error [msg f & {:keys [fail?]
                                 :or {fail? true}}]
      `(if-clj
         (try ~f
              (catch Throwable t#
                (if ~fail?
                  (throw (RuntimeException. ~msg t#))
                  {:f-failed (ex-info ~msg {} t#)})))
         (try ~f
              (catch :default t#
                (if ~fail?
                  (throw (~'str ~msg " " t#))
                  {:f-failed (ex-info ~msg {} t#)}))))))

#?(:clj
    (defmacro throw-runtime [msg]
      `(throw (if-clj (RuntimeException. ~msg)
                      (~'str ~msg)))))

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
