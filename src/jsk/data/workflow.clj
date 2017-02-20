(ns jsk.data.workflow
  (:require [jsk.data.db :as db]
            [jsk.data.models :as m]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [datomic.api :as d]
            [e85th.commons.datomic :as datomic]
            [e85th.commons.ex :as ex]))


(s/defn find-all :- [m/Workflow]
  [{:keys [db]}]
  (datomic/get-all-entities-with-attr db :workflow/name))

(s/defn find-by-id :- (s/maybe m/Workflow)
  "Finds an workflow by id."
  [{:keys [db]} workflow-id :- s/Int]
  (let [extract-ids (partial map :db/id)]
    (some-> (datomic/get-entity-with-attr db workflow-id :workflow/name)
            (update-in [:workflow/schedules] extract-ids)
            (update-in [:workflow/tags] extract-ids))))

(def ^{:doc "Same as find-by-id except throws NotFoundException if no such job."}
  find-by-id! (ex/wrap-not-found find-by-id))


(s/defn create :- m/Workflow
  "Creates a new workflow."
  [{:keys [db] :as res} workflow :- m/Workflow user-id :- s/Int]
  (let [workflow-id (d/tempid :db.part/jsk)
        facts [(-> workflow
                   (assoc :db/id workflow-id))]
        {:keys [db-after tempids]} @(d/transact (:cn db) facts)]
    (find-by-id res (d/resolve-tempid db-after tempids workflow-id))))

(s/defn modify :- m/Workflow
  "Updates and returns the new record."
  [{:keys [db] :as res} workflow-id :- s/Int workflow :- m/Workflow user-id :- s/Int]
  @(d/transact (:cn db) [(-> workflow
                             (assoc :db/id workflow-id))])
  (find-by-id! res workflow-id))

(s/defn rm
  "Delete an workflow."
  [{:keys [db]} workflow-id :- s/Int user-id :- s/Int]
  @(d/transact (:cn db) [[:db/retractEntity workflow-id]]))

(s/defn assoc-schedules :- m/WorkflowSchedules
  [{:keys [db] :as res} workflow-id :- s/Int schedule-ids :- [s/Int] user-id :- s/Int]
  @(d/transact (:cn db) [{:db/id workflow-id :workflow/schedules schedule-ids}])
  {:workflow/id workflow-id
   :schedule/ids (:workflow/schedules (find-by-id! res workflow-id))})

(s/defn dissoc-schedules :- m/WorkflowSchedules
  [{:keys [db] :as res} workflow-id :- s/Int schedule-ids :- [s/Int] user-id :- s/Int]
  (let [facts (for [s schedule-ids]
                [:db/retract workflow-id :workflow/schedules s])]
    @(d/transact (:cn db) facts)
    {:workflow/id workflow-id
     :schedule/ids (:workflow/schedules (find-by-id! res workflow-id))}))

(s/defn assoc-tags
  [{:keys [db] :as res} workflow-id :- s/Int tag-ids :- [s/Int] user-id :- s/Int]
  @(d/transact (:cn db) [{:db/id workflow-id :workflow/tags tag-ids}])
  {:workflow/id workflow-id
   :tag/ids (:workflow/tags (find-by-id! res workflow-id))})

(s/defn dissoc-tags
  [{:keys [db] :as res} workflow-id :- s/Int tag-ids :- [s/Int] user-id :- s/Int]
  (let [facts (for [t tag-ids]
                [:db/retract workflow-id :workflow/tags t])]
    @(d/transact (:cn db) facts))
  {:workflow/id workflow-id
   :tag/ids (:workflow/tags (find-by-id! res workflow-id))} )
