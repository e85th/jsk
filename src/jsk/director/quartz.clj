(ns jsk.director.quartz
  "Quartz Scheduler interface."
  (:require [clojurewerkz.quartzite.jobs :as qj :refer [defjob]]
            [clojurewerkz.quartzite.triggers :as qt]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.conversion :as qc]
            [clojurewerkz.quartzite.schedule.cron :as cron]
            [schema.core :as s]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log])
  (:import [org.quartz Job JobDetail JobKey Trigger TriggerKey Scheduler]))

(defonce job-handler (atom nil))

(defn job-key
  [id]
  (qj/key (str id) "jsk.job"))

;; this is a defrecord, quartz needs a no arg constructor for the class
(defjob JskJob
  [ctx]
  (let [{:strs [node-id]} (qc/from-job-data ctx)
        handler @job-handler]
    (assert handler "quartz/init! must be called.")
    (handler node-id)))

(defn job-detail
  "node-id must be unique across jobs and workflows"
  [node-id]
  (qj/build (qj/of-type JskJob)
            (qj/using-job-data {"node-id" node-id}) ; quartz requires string keys
            (qj/with-identity (job-key node-id))))

(s/defn cron-trigger
  [cron-expr]
  (qt/build
   (qt/start-now)
   (qt/with-schedule (cron/schedule (cron/cron-schedule cron-expr)))))

(defn job-key-exists?
  [^Scheduler scheduler ^JobKey k]
  (.checkExists scheduler k))

(defn delete-job-triggers
  [scheduler node-id]
  (->> (qs/get-triggers-of-job scheduler (job-key node-id))
       (qs/delete-triggers scheduler)))

(defn- schedule-job*
  "Schedules a job with multiple triggers."
  [^Scheduler scheduler ^JobDetail job triggers]
  (.scheduleJob scheduler job (set triggers) true))

(s/defn schedule-cron-job
  "Schedules a job to be triggered based on the cron expressions.
   A job can have many triggers point to it. A trigger can only point to one job."
  [scheduler node-id :- s/Int cron-exprs :- #{s/Str}]
  (log/infof "Scheduling quartz job for node-id: %s" node-id)
  (delete-job-triggers scheduler node-id)
  (schedule-job* scheduler (job-detail node-id) (map cron-trigger cron-exprs)))


(defn init!
  "Unfortunately have to be dirty and set state here to have director be fairly clean."
  [job-trigger-fn]
  (reset! job-handler job-trigger-fn))
