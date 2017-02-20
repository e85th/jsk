(ns jsk.data.schedule
  (:require [jsk.data.db :as db]
            [jsk.data.models :as m]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [datomic.api :as d]
            [e85th.commons.datomic :as datomic]
            [e85th.commons.ex :as ex])
  (:import [org.quartz CronExpression]))

(def bad-cron-expr-ex ::bad-cron-exper-ex)
(def duplicate-assoc-ex ::duplicate-assoc-ex)

(s/defn cron-expr? :- s/Bool
  "Answers if expr is a valid cron expression"
  [expr :- (s/maybe s/Str)]
  (true? (some-> expr CronExpression/isValidExpression)))

(s/defn validate-cron-expr
  [cron-expr :- (s/maybe s/Str)]
  (when-not (cron-expr? cron-expr)
    (throw (ex/new-validation-exception bad-cron-expr-ex "Invalid cron-expr."))))

(s/defn validate-create
  "Validates schedule creation"
  [{:keys [schedule/cron-expr]} :- m/Schedule]
  (validate-cron-expr cron-expr))

(s/defn validate-modify
  "Validates schedule update."
  [{:keys [schedule/cron-expr] :as sched} :- m/Schedule]
  ;; check cron-expr only if key is present via contains?
  (when (contains? sched :schedule/cron-expr)
    (validate-cron-expr cron-expr)))

(s/defn find-all :- [m/Schedule]
  [{:keys [db]}]
  (datomic/get-all-entities-with-attr db :schedule/name))

(s/defn find-by-id :- (s/maybe m/Schedule)
  "Finds an schedule by id."
  [{:keys [db]} schedule-id :- s/Int]
  (datomic/get-entity-with-attr db schedule-id :schedule/name))

(def ^{:doc "Same as find-by-id except throws NotFoundException if no such schedule."}
  find-by-id! (ex/wrap-not-found find-by-id))

;; -- create, update, delete
(s/defn create :- m/Schedule
  "Creates a new schedule."
  [{:keys [db] :as res} schedule :- m/Schedule user-id :- s/Int]
  (validate-create schedule)
  (let [schedule-id (d/tempid :db.part/jsk)
        facts [(assoc schedule :db/id schedule-id)]
        {:keys [db-after tempids]} @(d/transact (:cn db) facts)]
    (find-by-id res (d/resolve-tempid db-after tempids schedule-id))))

(s/defn modify :- m/Schedule
  "Updates and returns the new record."
  [{:keys [db] :as res} schedule-id :- s/Int schedule :- m/Schedule user-id :- s/Int]
  (validate-modify schedule)
  @(d/transact (:cn db) [(assoc schedule :db/id schedule-id)])
  (find-by-id! res schedule-id))

(s/defn rm
  "Delete an schedule."
  [{:keys [db]} schedule-id :- s/Int user-id :- s/Int]
  @(d/transact (:cn db) [[:db/retractEntity schedule-id]]))

;; ;;-- job schedule
;; (s/defn find-job-schedule-by-id :- (s/maybe m/JobScheduleInfo)
;;   [{:keys [db]} id :- s/Int]
;;   (db/select-job-schedule-by-id db id))

;; (def ^{:doc "Same as find-job-schedule-by-id except throws NotFoundException if no such job."}
;;   find-job-schedule-by-id! (ex/wrap-not-found find-job-schedule-by-id))

;; (s/defn find-job-schedule :- (s/maybe m/JobScheduleInfo)
;;   [{:keys [db]} job-id :- s/Int schedule-id :- s/Int]
;;   (db/select-job-schedule db job-id schedule-id))

;; (def ^{:doc "Same as find-job-schedule except throws NotFoundException if no such job."}
;;   find-job-schedule! (ex/wrap-not-found find-job-schedule))

;; (s/defn find-schedules-by-job-id :- [m/JobScheduleInfo]
;;   [{:keys [db]} id :- s/Int]
;;   (db/select-job-schedules-by-job-id db id))

;; ;;-- workflow schedule
;; (s/defn find-workflow-schedule-by-id :- (s/maybe m/WorkflowScheduleInfo)
;;   [{:keys [db]} id :- s/Int]
;;   (db/select-workflow-schedule-by-id db id))

;; (def ^{:doc "Same as find-workflow-schedule-by-id except throws NotFoundException if no such workflow."}
;;   find-workflow-schedule-by-id! (ex/wrap-not-found find-workflow-schedule-by-id))

;; (s/defn find-workflow-schedule :- (s/maybe m/WorkflowScheduleInfo)
;;   [{:keys [db]} workflow-id :- s/Int schedule-id :- s/Int]
;;   (db/select-workflow-schedule db workflow-id schedule-id))

;; (def ^{:doc "Same as find-workflow-schedule except throws NotFoundException if no such workflow."}
;;   find-workflow-schedule! (ex/wrap-not-found find-workflow-schedule))

;; (s/defn find-schedules-by-workflow-id :- [m/WorkflowScheduleInfo]
;;   [{:keys [db]} id :- s/Int]
;;   (db/select-workflow-schedules-by-workflow-id db id))



;; -- schedule job assoc, dissoc
;; (s/defn validate-job-assoc
;;   [res job-id :- s/Int schedule-id :- s/Int]
;;   (when (find-job-schedule res job-id schedule-id)
;;     (throw (ex/new-validation-exception duplicate-assoc-ex "Schedule association already exists."))))

;; (s/defn create-job-assoc :- m/JobScheduleInfo
;;   "Associate a job to a schedule."
;;   [{:keys [db] :as res}
;;    {:keys [job-id schedule-id]} :- m/ScheduleJobAssoc
;;    user-id :- s/Int]
;;   (validate-job-assoc res job-id schedule-id)
;;   (let [id (db/insert-job-schedule db job-id schedule-id user-id)]
;;     (find-job-schedule-by-id! res id)))

;; (s/defn remove-job-assoc :- s/Bool
;;   "Dissoc a job from a schedule."
;;   [{:keys [db]} id :- s/Int user-id :- s/Int]
;;   (db/delete-job-schedule db id)
;;   true)

;; ;; -- schedule workflow assoc, dissoc
;; (s/defn validate-workflow-assoc
;;   [res workflow-id :- s/Int schedule-id :- s/Int]
;;   (when (find-workflow-schedule res workflow-id schedule-id)
;;     (throw (ex/new-validation-exception duplicate-assoc-ex "Schedule association already exists."))))

;; (s/defn create-workflow-assoc :- m/WorkflowScheduleInfo
;;   "Associate a workflow to a schedule."
;;   [{:keys [db] :as res}
;;    {:keys [workflow-id schedule-id] :as sched-assoc} :- m/ScheduleWorkflowAssoc
;;    user-id :- s/Int]
;;   (log/infof "Create schedule-workflow assoc %s" sched-assoc)
;;   (validate-workflow-assoc res workflow-id schedule-id)
;;   (let [id (db/insert-workflow-schedule db workflow-id schedule-id user-id)]
;;     (log/infof "inserted row %s" id)
;;     (find-workflow-schedule-by-id! res id)))

;; (s/defn remove-workflow-assoc :- s/Bool
;;   "Dissoc a workflow from a schedule."
;;   [{:keys [db]} id :- s/Int user-id :- s/Int]
;;   (log/infof "Deleteing workflow schedule assoc id %s" id)
;;   (db/delete-workflow-schedule db id)
;;   true)
