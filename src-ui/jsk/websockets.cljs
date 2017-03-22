(ns jsk.websockets
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [e85th.ui.net.websockets :as websockets]
            [jsk.common.data :as data]
            [jsk.data.job.events :as job-events]
            [jsk.data.workflow.events :as workflow-events]
            [jsk.data.explorer.events :as explorer-events]
            [jsk.data.alert.events :as alert-events]
            [jsk.data.agent.events :as agent-events]
            [jsk.data.schedule.events :as schedule-events]
            [re-frame.core :as rf]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Refreshses explorer
(defn refresh-explorer
  [node-id]
  (rf/dispatch [explorer-events/refresh-node node-id]))

(def refresh-workflows (partial refresh-explorer explorer-events/executables-id))
(def refresh-jobs (partial refresh-explorer explorer-events/executables-id))
(def refresh-schedules (partial refresh-explorer explorer-events/schedules-id))
(def refresh-agents (partial refresh-explorer explorer-events/agents-id))
(def refresh-alerts (partial refresh-explorer explorer-events/alerts-id))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Refreshses entities that are currently in app db
(defn refresh-entity
  [refresh-event entity-id]
  (rf/dispatch [refresh-event entity-id]))

(def refresh-alert (partial refresh-entity alert-events/refresh-alert))
(def refresh-agent (partial refresh-entity agent-events/refresh-agent))
(def refresh-schedule (partial refresh-entity schedule-events/refresh-schedule))
(def refresh-job (partial refresh-entity job-events/refresh-job))
(def refresh-workflow (partial refresh-entity workflow-events/refresh-workflow))


(defmulti on-event first)

;; -- Job
(defmethod on-event :jsk.job/created
  [msg]
  (refresh-jobs))

(defmethod on-event :jsk.job/modified
  [[_ job-id]]
  (refresh-job job-id)
  (refresh-jobs))

(defmethod on-event :jsk.job/deleted
  [msg]
  (refresh-jobs))

;; -- Workflow
(defmethod on-event :jsk.workflow/created
  [msg]
  (refresh-workflows)
  (log/infof "Workflow created: %s" msg))

(defmethod on-event :jsk.workflow/modified
  [[_ workflow-id]]
  (refresh-workflow workflow-id)
  (refresh-workflows))

(defmethod on-event :jsk.workflow/deleted
  [msg]
  (refresh-workflows)
  (log/infof "Workflow deleted: %s" msg))

;; -- Schedules

(defmethod on-event :jsk.schedule/created
  [msg]
  (refresh-schedules))

(defmethod on-event :jsk.schedule/modified
  [[_ schedule-id]]
  (refresh-schedule schedule-id)
  (refresh-schedules))

(defmethod on-event :jsk.schedule/deleted
  [msg]
  (refresh-schedules))

;; -- Alerts
(defmethod on-event :jsk.alert/created
  [msg]
  (refresh-alerts))

(defmethod on-event :jsk.alert/modified
  [[_ alert-id]]
  (refresh-alert alert-id)
  (refresh-alerts))

(defmethod on-event :jsk.alert/deleted
  [msg]
  (refresh-alerts))

;; -- Agents
(defmethod on-event :jsk.agent/created
  [msg]
  (refresh-agents))

(defmethod on-event :jsk.agent/modified
  [[_ agent-id]]
  (refresh-agent agent-id)
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
