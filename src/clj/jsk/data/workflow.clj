(ns jsk.data.workflow
  (:require [jsk.data.db :as db]
            [jsk.data.models :as m]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [e85th.commons.ex :as ex]))


(s/defn find-by-id :- (s/maybe m/Workflow)
  "Finds an workflow by id."
  [{:keys [db]} workflow-id :- s/Int]
  (db/select-workflow-by-id db workflow-id))

(def ^{:doc "Same as find-by-id except throws NotFoundException if no such workflow."}
  find-by-id! (ex/wrap-not-found find-by-id))


(s/defn create :- m/Workflow
  "Creates a new workflow."
  [{:keys [db] :as res} new-workflow :- m/WorkflowCreate user-id :- s/Int]
  (let [workflow-id (db/insert-workflow db new-workflow user-id)]
    (log/infof "Created new workflow with id %s" workflow-id)
    (find-by-id res workflow-id)))

(s/defn amend :- m/Workflow
  "Updates and returns the new record."
  [{:keys [db] :as res} workflow-id :- s/Int workflow-update :- m/WorkflowAmend user-id :- s/Int]
  (db/update-workflow db workflow-id workflow-update user-id)
  (find-by-id! res workflow-id))

(s/defn delete
  "Delete an workflow."
  [{:keys [db]} workflow-id :- s/Int user-id :- s/Int]
  (db/delete-workflow db workflow-id))
