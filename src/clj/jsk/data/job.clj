(ns jsk.data.job
  (:require [jsk.data.db :as db]
            [jsk.data.models :as m]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [e85th.commons.ex :as ex]))


(s/defn find-by-id :- (s/maybe m/Job)
  "Finds an job by id."
  [{:keys [db]} job-id :- s/Int]
  (db/select-job-by-id db job-id))

(def ^{:doc "Same as find-by-id except throws NotFoundException if no such job."}
  find-by-id! (ex/wrap-not-found find-by-id))

(s/defn create :- m/Job
  "Creates a new job."
  [{:keys [db] :as res} new-job :- m/JobCreate user-id :- s/Int]
  (let [new-job (merge {:behavior-id m/standard-job-behavior-id} new-job)
        job-id (db/insert-job db new-job user-id)]
    (log/infof "Created new job with id %s" job-id)
    (find-by-id res job-id)))

(s/defn amend :- m/Job
  "Updates and returns the new record."
  [{:keys [db] :as res} job-id :- s/Int job-update :- m/JobAmend user-id :- s/Int]
  (db/update-job db job-id job-update user-id)
  (find-by-id! res job-id))

(s/defn delete
  "Delete an job."
  [{:keys [db]} job-id :- s/Int user-id :- s/Int]
  (db/delete-job db job-id))


(s/defn assoc-schedule :- s/Bool
  [{:keys [db]} job-id :- s/Int schedule-id :- s/Int user-id :- s/Int]
  (db/insert-job-schedule db job-id schedule-id user-id)
  true)

(s/defn dissoc-schedule :- s/Bool
  [{:keys [db]} id :- s/Int]
  (db/delete-job-schedule db id)
  true)
