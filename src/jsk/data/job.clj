(ns jsk.data.job
  (:require [jsk.data.db :as db]
            [jsk.data.models :as m]
            [jsk.data.alert :as alert]
            [jsk.data.tag :as tag]
            [jsk.data.schedule :as schedule]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [datomic.api :as d]
            [e85th.commons.datomic :as datomic]
            [clojure.edn :as edn]
            [e85th.commons.mq :as mq]
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
            (update-in [:job/alerts] extract-ids)
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

(s/defn find-info-by-id! :- m/JobInfo
  [{:keys [db] :as res} job-id :- s/Int]
  (let [job (find-by-id! res job-id)]
    (assoc job
           :job/alerts (alert/find-by-ids res (:job/alerts job))
           :job/schedules (schedule/find-by-ids res (:job/schedules job))
           :job/tags (tag/find-by-ids res (:job/tags job)))))


(s/defn create :- s/Int
  "Creates a new job."
  ([res user-id]
   (create res (m/new-job) user-id))
  ([{:keys [db publisher] :as res} job :- m/Job user-id :- s/Int]
   (let [temp-id (d/tempid :db.part/jsk)
         facts [(-> job
                    (assoc :db/id temp-id)
                    (encode-job-props))]
         {:keys [db-after tempids]} @(d/transact (:cn db) facts)
         job-id (d/resolve-tempid db-after tempids temp-id)]
     (mq/publish publisher [:jsk.job/created job-id])
     job-id)))

(s/defn modify
  "Updates the record."
  [{:keys [db publisher] :as res} job-id :- s/Int job :- m/Job user-id :- s/Int]
  @(d/transact (:cn db) [(-> job
                             (assoc :db/id job-id)
                             (encode-job-props))])
  (mq/publish publisher [:jsk.job/modified job-id]))

(s/defn rm
  "Delete an job."
  [{:keys [db publisher]} job-id :- s/Int user-id :- s/Int]
  @(d/transact (:cn db) [[:db/retractEntity job-id]])
  (mq/publish publisher [:jsk.job/deleted job-id]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schedule Assoc
(s/defn assoc-schedules :- m/JobSchedules
  [{:keys [db publisher] :as res} job-id :- s/Int schedule-ids :- [s/Int] user-id :- s/Int]
  @(d/transact (:cn db) [{:db/id job-id :job/schedules schedule-ids}])
  (mq/publish publisher [:jsk.job/modified job-id])
  {:job/id job-id
   :schedule/ids (:job/schedules (find-by-id! res job-id))})

(s/defn dissoc-schedules :- m/JobSchedules
  [{:keys [db publisher] :as res} job-id :- s/Int schedule-ids :- [s/Int] user-id :- s/Int]
  (let [facts (for [s schedule-ids]
                [:db/retract job-id :job/schedules s])]
    @(d/transact (:cn db) facts)
    (mq/publish publisher [:jsk.job/modified job-id])
    {:job/id job-id
     :schedule/ids (:job/schedules (find-by-id! res job-id))}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Alert Assoc
(s/defn assoc-alerts :- m/JobAlerts
  [{:keys [db publisher] :as res} job-id :- s/Int alert-ids :- [s/Int] user-id :- s/Int]
  @(d/transact (:cn db) [{:db/id job-id :job/alerts alert-ids}])
  (mq/publish publisher [:jsk.job/modified job-id])
  {:job/id job-id
   :alert/ids (:job/alerts (find-by-id! res job-id))})

(s/defn dissoc-alerts :- m/JobAlerts
  [{:keys [db publisher] :as res} job-id :- s/Int alert-ids :- [s/Int] user-id :- s/Int]
  (let [facts (for [s alert-ids]
                [:db/retract job-id :job/alerts s])]
    @(d/transact (:cn db) facts)
    (mq/publish publisher [:jsk.job/modified job-id])
    {:job/id job-id
     :alert/ids (:job/alerts (find-by-id! res job-id))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tag Assoc
(s/defn assoc-tags :- m/JobTags
  [{:keys [db publisher] :as res} job-id :- s/Int tag-ids :- [s/Int] user-id :- s/Int]
  @(d/transact (:cn db) [{:db/id job-id :job/tags tag-ids}])
  (mq/publish publisher [:jsk.job/modified job-id])
  {:job/id job-id
   :tag/ids (:job/tags (find-by-id! res job-id))})

(s/defn dissoc-tags :- m/JobTags
  [{:keys [db publisher] :as res} job-id :- s/Int tag-ids :- [s/Int] user-id :- s/Int]
  (let [facts (for [t tag-ids]
                [:db/retract job-id :job/tags t])]
    @(d/transact (:cn db) facts))
  (mq/publish publisher [:jsk.job/modified job-id])
  {:job/id job-id
   :tag/ids (:job/tags (find-by-id! res job-id))} )
