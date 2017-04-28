(ns jsk.data.schedule
  (:require [jsk.data.db :as db]
            [jsk.data.models :as m]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [datomic.api :as d]
            [e85th.commons.datomic :as datomic]
            [e85th.commons.mq :as mq]
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
    (throw (ex/validation bad-cron-expr-ex "Invalid cron-expr."))))

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

(s/defn find-by-ids :- [m/Schedule]
  [{:keys [db]} schedule-ids :- [s/Int]]
  (datomic/get-entities-with-attr-by-ids db :schedule/name schedule-ids))

;; -- create, update, delete
(s/defn create :- s/Int
  "Creates a new schedule."
  ([res user-id]
   (create res (m/new-schedule) user-id))
  ([{:keys [db publisher] :as res} schedule :- m/Schedule user-id :- s/Int]
   (validate-create schedule)
   (let [temp-id (d/tempid :db.part/jsk)
         facts [(assoc schedule :db/id temp-id)]
         {:keys [db-after tempids]} @(d/transact (:cn db) facts)
         schedule-id (d/resolve-tempid db-after tempids temp-id)]
     (mq/publish publisher [:jsk.schedule/created schedule-id])
     schedule-id)))

(s/defn modify
  "Updates and returns the new record."
  [{:keys [db publisher] :as res} schedule-id :- s/Int schedule :- m/Schedule user-id :- s/Int]
  (validate-modify schedule)
  @(d/transact (:cn db) [(assoc schedule :db/id schedule-id)])
  (mq/publish publisher [:jsk.schedule/modified schedule-id]))

(s/defn rm
  "Delete an schedule."
  [{:keys [db publisher]} schedule-id :- s/Int user-id :- s/Int]
  @(d/transact (:cn db) [[:db/retractEntity schedule-id]])
  (mq/publish publisher [:jsk.schedule/deleted schedule-id]))
