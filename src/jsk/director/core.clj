(ns jsk.director.core
  "Coordination of workflows."
  (:require [taoensso.timbre :as log]
            [jsk.director.quartz :as qz]
            [jsk.data.job :as job]
            [jsk.director.db :as db]
            [jsk.data.workflow :as wf]
            [jsk.data.schedule :as schedule]))


(defn job-triggered
  "Fetch the job/workflow and create the execution graph, save it to the db. Then load that execution graph."
  [res node-id]
  (log/infof "job triggered for node-id: %s" node-id))

(defn schedule-everything
  "Fetches jobs and workflows with schedules.  Creates triggers for execution."
  [{:keys [scheduler db] :as res}]
  ;; get all jobs and workflows with cron schedules attached
  ;; job-id -> [cron-exprs]
  (doseq [[node-id cron-exprs] (db/find-node-schedules db)]
    (qz/schedule-cron-job scheduler node-id cron-exprs)))

(defn init
  "Starts up the director. Fetches all schedules attached to jobs/workflows and
   adds them to the quartz scheduler."
  [res]
  (log/infof "Director initializing")
  (qz/init! (partial job-triggered res))
  #_(schedule-everything res))
