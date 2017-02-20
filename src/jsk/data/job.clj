(ns jsk.data.job
  (:require [jsk.data.db :as db]
            [jsk.data.models :as m]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [datomic.api :as d]
            [e85th.commons.datomic :as datomic]
            [clojure.edn :as edn]
            [e85th.commons.ex :as ex]))



(defn encode-job-props
  [job]
  (cond-> job
    (contains? job :job/props) (update-in [:job/props] pr-str)))

(defn as-job
  "Converts data retrieved from the database to fit Job schema."
  [job-data]
  (let [extract-ids (partial map :db/id)]
    (some-> job-data
            (update-in [:job/props] edn/read-string)
            (update-in [:job/schedules] extract-ids)
            (update-in [:job/tags] extract-ids))))

(s/defn find-all :- [m/Job]
  [{:keys [db]}]
  (->> (datomic/get-all-entities-with-attr db :job/name)
       (map as-job)))

(s/defn find-by-id :- (s/maybe m/Job)
  "Finds an job by id."
  [{:keys [db]} job-id :- s/Int]
  (as-job (datomic/get-entity-with-attr db job-id :job/name)))

(def ^{:doc "Same as find-by-id except throws NotFoundException if no such job."}
  find-by-id! (ex/wrap-not-found find-by-id))

(s/defn create :- m/Job
  "Creates a new job."
  [{:keys [db] :as res} job :- m/Job user-id :- s/Int]
  (let [job-id (d/tempid :db.part/jsk)
        facts [(-> job
                   (assoc :db/id job-id)
                   (encode-job-props))]
        {:keys [db-after tempids]} @(d/transact (:cn db) facts)]
    (find-by-id res (d/resolve-tempid db-after tempids job-id))))

(s/defn modify :- m/Job
  "Updates and returns the new record."
  [{:keys [db] :as res} job-id :- s/Int job :- m/Job user-id :- s/Int]
  @(d/transact (:cn db) [(-> job
                             (assoc :db/id job-id)
                             (encode-job-props))])
  (find-by-id! res job-id))

(s/defn rm
  "Delete an job."
  [{:keys [db]} job-id :- s/Int user-id :- s/Int]
  @(d/transact (:cn db) [[:db/retractEntity job-id]]))

(s/defn assoc-schedules :- m/JobSchedules
  [{:keys [db] :as res} job-id :- s/Int schedule-ids :- [s/Int] user-id :- s/Int]
  @(d/transact (:cn db) [{:db/id job-id :job/schedules schedule-ids}])
  {:job/id job-id
   :schedule/ids (:job/schedules (find-by-id! res job-id))})

(s/defn dissoc-schedules :- m/JobSchedules
  [{:keys [db] :as res} job-id :- s/Int schedule-ids :- [s/Int] user-id :- s/Int]
  (let [facts (for [s schedule-ids]
                [:db/retract job-id :job/schedules s])]
    @(d/transact (:cn db) facts)
    {:job/id job-id
     :schedule/ids (:job/schedules (find-by-id! res job-id))}))

(s/defn assoc-tags
  [{:keys [db] :as res} job-id :- s/Int tag-ids :- [s/Int] user-id :- s/Int]
  @(d/transact (:cn db) [{:db/id job-id :job/tags tag-ids}])
  {:job/id job-id
   :tag/ids (:job/tags (find-by-id! res job-id))})

(s/defn dissoc-tags
  [{:keys [db] :as res} job-id :- s/Int tag-ids :- [s/Int] user-id :- s/Int]
  (let [facts (for [t tag-ids]
                [:db/retract job-id :job/tags t])]
    @(d/transact (:cn db) facts))
  {:job/id job-id
   :tag/ids (:job/tags (find-by-id! res job-id))} )
