(ns mount.tools.graph)

#?(:clj
    ;;TODO ns based for now. need to be _state_ based. or better yet need to have a real graph :)
    (defn- add-deps [{:keys [ns] :as state} states]
      (let [refers (ns-refers ns)
            any (->> states vals (map :var) set)
            deps (->> (filter (comp any val) refers)
                      (map (comp str second))
                      set)]
        (assoc (dissoc state :ns)
               :deps deps))))

#?(:clj
    (defn- meta-with-ns [[sname {:keys [var] :as smeta}]]
      (let [sns (-> var meta :ns)]
        (assoc smeta :ns sns :name sname))))

#?(:clj
    (defn states-with-deps []
      (let [states @@#'mount.core/meta-state]
        (->> (map (comp #(add-deps % states)
                        #(select-keys % [:name :order :ns :status])
                        meta-with-ns)
                  states)
             (sort-by :order)))))

