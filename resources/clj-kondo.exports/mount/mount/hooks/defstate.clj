(ns hooks.defstate
  (:require [clj-kondo.hooks-api :as api]))

(defn defstate [{:keys [node]}]
  (let [[n & args] (next (:children node))
        [docs args] (if (string? (api/sexpr (first args)))
                      [(first args) (next args)]
                      [nil args])
        m (when-let [m (first (:meta n))]
            (api/sexpr m))
        m (if (map? m) m {})
        ks (cond-> (take 1 args)
                   (> (count args) 2) (conj (nth args 2)))
        invalid-key (first (remove (comp (partial contains? #{:start :stop}) api/sexpr) ks))]
    (cond
      invalid-key
      (api/reg-finding!
        {:message (str "lifecycle functions can only contain `:start` and `:stop`. illegal function found: " (api/sexpr invalid-key))
         :type    :mount/defstate
         :row     (:row (meta invalid-key))
         :col     (:col (meta invalid-key))})
      (not (contains? (set (map api/sexpr ks)) :start))
      (throw (ex-info "lifecycle functions must include `:start`" {}))
      ((complement contains?) #{2 4} (count args))
      (throw (ex-info "lifecycle functions must consist of no more than 2 pair forms: `:start` and `:stop`" {}))
      (and (contains? m :on-reload) (not (contains? #{:noop :stop} (:on-reload m))))
      (api/reg-finding!
        {:message "metadata `:on-reload` key can only have value of `noop` or `stop`"
         :type    :mount/defstate
         :row     (:row (meta n))
         :col     (:col (meta n))})
      :else
      ;; A state's value is the result of `:start`, never `:stop`. clj-kondo
      ;; infers the var's type from the LAST expression of the generated `do`,
      ;; so emitting the `:stop` body last made it adopt the stop fn's return
      ;; type (typically `nil`) -- producing false `Expected: <X>, received: nil`
      ;; at deref / typed call sites (see #141). Order the lifecycle pairs so the
      ;; `:start` body is evaluated last; the `:stop` body is still analysed.
      (let [start? (fn [pair] (= :start (api/sexpr (first pair))))
            pairs  (partition 2 args)
            args   (mapcat identity (concat (remove start? pairs)
                                            (filter start? pairs)))]
        {:node (api/list-node
                (cond-> [(api/token-node 'def) n]
                  docs (conj docs)
                  true (conj (api/list-node
                              (list*
                               (api/token-node 'do)
                               args)))))}))))
