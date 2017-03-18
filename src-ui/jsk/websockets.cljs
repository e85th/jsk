(ns jsk.websockets
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [e85th.ui.net.websockets :as websockets]
            [jsk.common.data :as data]
            [jsk.data.job.events :as job-events]
            ;[jsk.data.workflow.events :as workflow-events]
            [jsk.data.alert.events :as alert-events]
            [jsk.data.agent.events :as agent-events]
            [jsk.data.schedule.events :as schedule-events]
            [re-frame.core :as rf]))

;(def refresh-workflows (partial rf/dispatch [workflow-events/fetch-workflow-list]))
(def refresh-jobs (partial rf/dispatch [job-events/fetch-job-list]))
(def refresh-schedules (partial rf/dispatch [schedule-events/fetch-schedule-list]))
(def refresh-agents (partial rf/dispatch [agent-events/fetch-agent-list]))
(def refresh-alerts (partial rf/dispatch [alert-events/fetch-alert-list]))

(defmulti on-event first)

;; -- Job
(defmethod on-event :jsk.job/created
  [msg]
  (refresh-jobs))

(defmethod on-event :jsk.job/modified
  [msg]
  (refresh-jobs))

(defmethod on-event :jsk.job/deleted
  [msg]
  (refresh-jobs))

;; -- Workflow
(defmethod on-event :jsk.workflow/created
  [msg]
  (log/infof "Workflow created: %s" msg))

(defmethod on-event :jsk.workflow/modified
  [msg]
  (log/infof "Workflow modified: %s" msg))

(defmethod on-event :jsk.workflow/deleted
  [msg]
  (log/infof "Workflow deleted: %s" msg))

;; -- Schedules

(defmethod on-event :jsk.schedule/created
  [msg]
  (refresh-schedules))

(defmethod on-event :jsk.schedule/modified
  [msg]
  (refresh-schedules))

(defmethod on-event :jsk.schedule/deleted
  [msg]
  (refresh-schedules))

;; -- Alerts
(defmethod on-event :jsk.alert/created
  [msg]
  (refresh-alerts)
  (log/infof "Alert created: %s" msg))

(defmethod on-event :jsk.alert/modified
  [[_ alert-id]]
  (refresh-alerts)
  (rf/dispatch [alert-events/refresh-alert alert-id])
  (log/infof "Alert modified: %s" alert-id))

(defmethod on-event :jsk.alert/deleted
  [msg]
  (refresh-alerts)
  (log/infof "Alert deleted: %s" msg))

;; -- Agents
(defmethod on-event :jsk.agent/created
  [msg]
  (refresh-agents))

(defmethod on-event :jsk.agent/modified
  [msg]
  (refresh-agents))

(defmethod on-event :jsk.agent/deleted
  [msg]
  (refresh-agents))

(defmethod on-event :default
  [msg]
  (log/infof "Ignoring message: %s" msg))
(defn init!
  "Call only after data/jsk-token is available."
  []
  (websockets/register-listener! on-event)
  (websockets/init! "/api/v1/chsk" {:type :auto
                                    :host (data/ws-host)
                                    :params {:token (data/jsk-token)}}))
