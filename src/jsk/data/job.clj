(ns jsk.data.job
  (:require [jsk.data.db :as db]
            [jsk.data.models :as m]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [datomic.api :as d]
            [e85th.commons.datomic :as datomic]
            [clojure.edn :as edn]
            [e85th.commons.ex :as ex]))


(s/defn find-by-id :- (s/maybe m/Job)
  "Finds an job by id."
  [{:keys [db]} job-id :- s/Int]
  (some-> (datomic/get-entity-with-attr db job-id :job/name)
          (update-in [:job/props] edn/read-string)))

(def ^{:doc "Same as find-by-id except throws NotFoundException if no such job."}
  find-by-id! (ex/wrap-not-found find-by-id))

(defn encode-job-props
  [job]
  (cond-> job
    (contains? job :job/props) (update-in [:job/props] pr-str)))

(s/defn create :- m/Job
  "Creates a new job."
  [{:keys [db] :as res} job :- m/Job user-id :- s/Int]
  (let [job-id (d/tempid :db.part/jsk)
        facts [(-> job
                   (assoc :db/id job-id)
                   (dissoc :job/tags);; causes an error, tempid something
                   (encode-job-props))]
        _ (log/infof "facts: %s" facts)
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


(s/defn add-schedules
  [{:keys [db]} job-id :- s/Int schedule-ids :- [s/Int] user-id :- s/Int]
  @(d/transact (:cn db) [[:db/add job-id :job/schedules schedule-ids]]))

(s/defn rm-schedules
  [{:keys [db]} job-id :- s/Int schedule-ids :- [s/Int] user-id :- s/Int]
  @(d/transact (:cn db) [[:db/retract job-id :job/schedules schedule-ids]]))

(s/defn add-tags
  [{:keys [db]} job-id :- s/Int tag-ids :- [s/Int] user-id :- s/Int]
  @(d/transact (:cn db) [[:db/add job-id :job/tags tag-ids]]))

(s/defn rm-tags
  [{:keys [db]} job-id :- s/Int tag-ids :- [s/Int] user-id :- s/Int]
  @(d/transact (:cn db) [[:db/retract job-id :job/tags tag-ids]]))
