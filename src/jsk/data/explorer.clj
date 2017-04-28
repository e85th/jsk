(ns jsk.data.explorer
  (:require [jsk.data.db :as db]
            [jsk.data.models :as m]
            [jsk.data.job :as job]
            [jsk.data.workflow :as workflow]
            [jsk.data.schedule :as schedule]
            [jsk.data.alert :as alert]
            [jsk.data.agent :as agent]
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
    :else (ex/validation :explorer/invalid-node-type "Unknown node type.")))


(s/defn create-node
  [res type :- s/Keyword user-id :- s/Int]
  (case type
    :alert (alert/create res user-id)
    :agent (agent/create res user-id)
    :job (job/create res user-id)
    :workflow (workflow/create res user-id)
    :schedule (schedule/create res user-id)
    (throw (ex/validation :explorer/invalid-node-type "Invalid node type for create."))))

(s/defn rm-node
  [res type :- s/Keyword id :- s/Int user-id :- s/Int]
  (case type
    :alert (alert/rm res id user-id)
    :agent (agent/rm res id user-id)
    :job (job/rm res id user-id)
    :workflow (workflow/rm res id user-id)
    :schedule (schedule/rm res id user-id)
    (throw (ex/validation :explorer/invalid-node-type "Invalid node type for delete."))))
