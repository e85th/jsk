(ns jsk.data.workflow
  (:require [jsk.data.db :as db]
            [jsk.data.models :as m]
            [jsk.data.alert :as alert]
            [jsk.data.tag :as tag]
            [jsk.data.schedule :as schedule]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [datomic.api :as d]
            [e85th.commons.datomic :as datomic]
            [e85th.commons.mq :as mq]
            [e85th.commons.ex :as ex]))

(def extract-ids (partial map :db/id))

(defn as-workflow-nodes
  [nodes]
  (map (fn [n]
         (-> ;(dissoc n :workflow.node/item :db/id)
             (assoc n :workflow.node/referent (-> n :workflow.node/referent :db/id))
             (update :workflow.node/successors extract-ids)
             (update :workflow.node/successors-err extract-ids)))
       nodes))

(defn as-workflow
  "Converts data retrieved from the database to fit Workflow schema."
  [workflow-data]
  (some-> workflow-data
          (update-in [:workflow/alerts] extract-ids)
          (update-in [:workflow/nodes] as-workflow-nodes)
          (update-in [:workflow/schedules] extract-ids)
          (update-in [:workflow/tags] extract-ids)))

(s/defn find-nodes-info
  "Finds all the jobs and workflow with the specified entity ids."
  [{:keys [db]} nodes]
  (let [db (-> db :cn d/db)]
    (for [{:keys [workflow.node/referent] :as node} nodes
          :let [entity (d/entity db referent)
                info (if (contains? entity :job/name)
                       {:workflow.node/name (:job/name entity)
                        :workflow.node/type :job}
                       {:workflow.node/name (:workflow/name entity)
                        :workflow.node/type :workflow})]]
      (merge node info))))

(s/defn find-all :- [m/Workflow]
  [{:keys [db]}]
  (->> (datomic/get-all-entities-with-attr db :workflow/name)
       (map as-workflow)))

(s/defn find-by-id ;:- (s/maybe m/Workflow)
  "Finds an workflow by id."
  [{:keys [db]} workflow-id :- s/Int]
  (as-workflow (datomic/get-entity-with-attr db workflow-id :workflow/name)))

(def ^{:doc "Same as find-by-id except throws NotFoundException if no such job."}
  find-by-id! (ex/wrap-not-found find-by-id))


(s/defn find-info-by-id! :- m/WorkflowInfo
  [{:keys [db] :as res} workflow-id :- s/Int]
  (let [workflow (find-by-id! res workflow-id)]
    (assoc workflow
           :workflow/alerts (alert/find-by-ids res (:workflow/alerts workflow))
           :workflow/nodes (find-nodes-info res (:workflow/nodes workflow))
           :workflow/schedules (schedule/find-by-ids res (:workflow/schedules workflow))
           :workflow/tags (tag/find-by-ids res (:workflow/tags workflow)))))

(s/defn create :- s/Int
  "Creates a new workflow."
  ([res user-id]
   (create res (m/new-workflow) user-id))
  ([{:keys [db publisher] :as res} workflow :- m/Workflow user-id :- s/Int]
   (let [temp-id (d/tempid :db.part/jsk)
         facts [(assoc workflow :db/id temp-id)]
         {:keys [db-after tempids]} @(d/transact (:cn db) facts)
         workflow-id (d/resolve-tempid db-after tempids temp-id)]
     (mq/publish publisher [:jsk.workflow/created workflow-id])
     workflow-id)))

(s/defn retract-node-facts
  "Returns a retract for each node for the workflow-id"
  [{:keys [workflow/nodes]} :- m/Workflow]
  (for [{:keys [db/id]} nodes]
    [:db/retractEntity id]))

(s/defn normalize-node-inputs
  [nodes :- [m/WorkflowNodeInput]]
  (let [node-id->temp-id (zipmap (map :workflow.node/id nodes)
                                 (repeatedly #(d/tempid :db.part/jsk)))
        to-ids #(map node-id->temp-id %)]
    (for [n nodes]
      (-> (dissoc n :workflow.node/id)
          (assoc :db/id (node-id->temp-id (:workflow.node/id n)))
          (update :workflow.node/successors to-ids)
          (update :workflow.node/successors-err to-ids)))))

;; retractEntity each current workflow nodes
;; add new workflow nodes
;; add references to current
(s/defn modify
  "Updates and returns the new record."
  [{:keys [db publisher] :as res} workflow-id :- s/Int workflow :- m/Workflow user-id :- s/Int]
  (let [cur (find-by-id! res workflow-id)
        workflow (assoc workflow :db/id workflow-id)
        workflow (if-let [nodes-input (-> workflow :workflow/nodes-input seq)]
                   (assoc workflow :workflow/nodes (normalize-node-inputs nodes-input))
                   workflow)
        workflow (dissoc workflow :workflow/nodes-input)
        facts (conj (retract-node-facts cur)
                    (assoc workflow :db/id workflow-id))
        _ (log/infof "workflow: %s" workflow)
        {:keys [db-after temp-ids]} @(d/transact (:cn db) facts)]
    (mq/publish publisher [:jsk.workflow/modified workflow-id])))

(s/defn rm
  "Delete an workflow."
  [{:keys [db publisher]} workflow-id :- s/Int user-id :- s/Int]
  @(d/transact (:cn db) [[:db/retractEntity workflow-id]])
  (mq/publish publisher [:jsk.workflow/deleted workflow-id]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schedule Assoc
(s/defn assoc-schedules :- m/WorkflowSchedules
  [{:keys [db publisher] :as res} workflow-id :- s/Int schedule-ids :- [s/Int] user-id :- s/Int]
  @(d/transact (:cn db) [{:db/id workflow-id :workflow/schedules schedule-ids}])
  (mq/publish publisher [:jsk.workflow/modified workflow-id])
  {:workflow/id workflow-id
   :schedule/ids (:workflow/schedules (find-by-id! res workflow-id))})

(s/defn dissoc-schedules :- m/WorkflowSchedules
  [{:keys [db publisher] :as res} workflow-id :- s/Int schedule-ids :- [s/Int] user-id :- s/Int]
  (let [facts (for [s schedule-ids]
                [:db/retract workflow-id :workflow/schedules s])]
    (mq/publish publisher [:jsk.workflow/modified workflow-id])
    @(d/transact (:cn db) facts)
    {:workflow/id workflow-id
     :schedule/ids (:workflow/schedules (find-by-id! res workflow-id))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Alert Assoc
(s/defn assoc-alerts :- m/WorkflowAlerts
  [{:keys [db publisher] :as res} workflow-id :- s/Int alert-ids :- [s/Int] user-id :- s/Int]
  @(d/transact (:cn db) [{:db/id workflow-id :workflow/alerts alert-ids}])
  (mq/publish publisher [:jsk.workflow/modified workflow-id])
  {:workflow/id workflow-id
   :alert/ids (:workflow/alerts (find-by-id! res workflow-id))})

(s/defn dissoc-alerts :- m/WorkflowAlerts
  [{:keys [db publisher] :as res} workflow-id :- s/Int alert-ids :- [s/Int] user-id :- s/Int]
  (let [facts (for [s alert-ids]
                [:db/retract workflow-id :workflow/alerts s])]
    @(d/transact (:cn db) facts)
    (mq/publish publisher [:jsk.workflow/modified workflow-id])
    {:workflow/id workflow-id
     :alert/ids (:workflow/alerts (find-by-id! res workflow-id))}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tag Assoc
(s/defn assoc-tags
  [{:keys [db publisher] :as res} workflow-id :- s/Int tag-ids :- [s/Int] user-id :- s/Int]
  @(d/transact (:cn db) [{:db/id workflow-id :workflow/tags tag-ids}])
  (mq/publish publisher [:jsk.workflow/modified workflow-id])
  {:workflow/id workflow-id
   :tag/ids (:workflow/tags (find-by-id! res workflow-id))})

(s/defn dissoc-tags
  [{:keys [db publisher] :as res} workflow-id :- s/Int tag-ids :- [s/Int] user-id :- s/Int]
  (let [facts (for [t tag-ids]
                [:db/retract workflow-id :workflow/tags t])]
    @(d/transact (:cn db) facts))
  (mq/publish publisher [:jsk.workflow/modified workflow-id])
  {:workflow/id workflow-id
   :tag/ids (:workflow/tags (find-by-id! res workflow-id))} )
