(ns jsk.websockets
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [e85th.ui.net.websockets :as websockets]
            [jsk.common.data :as data]
            [re-frame.core :as rf]))

(defmulti on-event first)

;; -- Job
(defmethod on-event :jsk.job/created
  [msg]
  (log/infof "Job created: %s" msg))

(defmethod on-event :jsk.job/modified
  [msg]
  (log/infof "Job modified: %s" msg))

(defmethod on-event :jsk.job/deleted
  [msg]
  (log/infof "Job deleted: %s" msg))

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
  (log/infof "Schedule created: %s" msg))

(defmethod on-event :jsk.schedule/modified
  [msg]
  (log/infof "Schedule modified: %s" msg))

(defmethod on-event :jsk.schedule/deleted
  [msg]
  (log/infof "Schedule deleted: %s" msg))

;; -- Alerts
(defmethod on-event :jsk.alert/created
  [msg]
  (log/infof "Alert created: %s" msg))

(defmethod on-event :jsk.alert/modified
  [msg]
  (log/infof "Alert modified: %s" msg))

(defmethod on-event :jsk.alert/deleted
  [msg]
  (log/infof "Alert deleted: %s" msg))

;; -- Agents
(defmethod on-event :jsk.agent/created
  [msg]
  (log/infof "Agent created: %s" msg))

(defmethod on-event :jsk.agent/modified
  [msg]
  (log/infof "Agent modified: %s" msg))

(defmethod on-event :jsk.agent/deleted
  [msg]
  (log/infof "Agent deleted: %s" msg))

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
