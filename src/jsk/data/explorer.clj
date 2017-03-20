(ns jsk.data.explorer
  (:require [jsk.data.db :as db]
            [jsk.data.models :as m]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [datomic.api :as d]
            [e85th.commons.datomic :as datomic]
            [e85th.commons.ex :as ex]))

(def node-type->field
  {:alert :alert/name
   :schedule :schedule/name
   :agent :agent/name
   :tag :tag/name})

(s/defn get-nodes* :- [m/ExplorerNode]
  "Gets the elements for the tree. alerts, agents, schedules and jobs."
  [{:keys [db]} type :- s/Keyword field :- s/Keyword]
  (->> (d/q '[:find ?id ?name ?type
              :in $ ?type ?field
              :where [?id ?field ?name]]
            (-> db :cn d/db)
            type field)
       (map (partial zipmap [:node/id :node/name :node/type]))))


(s/defn get-executable-nodes
  [res]
  (concat (get-nodes* res :job :job/name)
          (get-nodes* res :workflow :workflow/name)))

(s/defn get-nodes :- [m/ExplorerNode]
  [res node-type :- s/Keyword]
  (log/infof "Get nodes for type: %s" node-type)
  (cond
    (= :executable node-type) (get-executable-nodes res)
    (contains? node-type->field node-type) (get-nodes* res node-type (node-type->field node-type))
    :else (ex/new-validation-exception :explorer/invalid-node-type "Unknown node type.")))
