(ns jsk.data.job.events
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [jsk.data.job.models :as m]
            [re-frame.core :as rf]
            [jsk.net.api :as api]
            [e85th.ui.util :as u]
            [e85th.ui.rf.macros :refer-macros [defevent-db defevent-fx defevent]]
            [clojure.string :as str]))

(defevent current-name-changed m/current-name)
(defevent current-desc-changed m/current-desc)
(defevent current-enabled?-changed m/current-enabled?)

(defevent-fx rpc-err
  [{:keys [db] :as cofx} event-v]
  (log/warnf "rpc err: %s" event-v)
  (let [[db msg] (condp = (second event-v)
                   :rpc/fetch-job-err [db "Error fetching job."]
                   :rpc/fetch-job-types-err [db "Error fetching job types."]
                   :rpc/save-job-err [db "Error saving job."]
                   :rpc/assoc-job-alerts-err [db "Error associating the alert."]
                   :rpc/dissoc-job-alerts-err [db "Error dissociating the alert."]
                   :rpc/assoc-job-schedules-err [db "Error associating the schedule."]
                   :rpc/dissoc-job-schedules-err [db "Error dissociating the schedule."]
                   :else [db "Encountered unhandled RPC error."])]
    {:db (assoc-in db m/busy? false)
     :notify [:alert {:message msg}]}))

(defevent-db no-op-ok
  [db _]
  db)

(defevent fetch-job-ok m/current)

(defevent-fx fetch-job
  [_ [_ job-id]]
  {:http-xhrio (api/fetch-job job-id fetch-job-ok [rpc-err :rpc/fetch-job-err])})

(defevent-db save-job-ok
  [db [_ job]]
  (-> db
      (assoc-in m/current job)
      (assoc-in m/busy? false)))

(defevent-fx save-job
  [{:keys [db]} _]
  (let [job (m/prep-for-save (get-in db m/current))]
    {:db (assoc-in db m/busy? true)
     :http-xhrio (api/save-job job save-job-ok [rpc-err :rpc/save-job-err])}))


(defevent-db current-props-field-value-changed
  [db [_ field-key field-value]]
  (assoc-in db (conj m/current-props field-key) field-value))

(defevent fetch-job-types-ok m/job-types)

(defevent-fx fetch-job-types
  [_ _]
  {:http-xhrio (api/fetch-job-types fetch-job-types-ok [rpc-err :rpc/fetch-job-types-err])})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schedule Assoc
(defevent-fx assoc-job-schedule
  [{:keys [db]} [_ schedule-id]]
  (let [job-id (get-in db m/current-id)]
    {:http-xhrio (api/assoc-job-schedules job-id [schedule-id] no-op-ok [rpc-err :rpc/assoc-job-schedules-err])}))

(defevent-fx dissoc-job-schedule
  [{:keys [db]} [_ schedule-id]]
  (let [job-id (get-in db m/current-id)]
    {:http-xhrio (api/dissoc-job-schedules job-id [schedule-id] no-op-ok [rpc-err :rpc/dissoc-job-schedules-err])}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Alert Assoc
(defevent-fx assoc-job-alert
  [{:keys [db]} [_ alert-id]]
  (let [job-id (get-in db m/current-id)]
    {:http-xhrio (api/assoc-job-alerts job-id [alert-id] no-op-ok [rpc-err :rpc/assoc-job-alerts-err])}))

(defevent-fx dissoc-job-alert
  [{:keys [db]} [_ alert-id]]
  (let [job-id (get-in db m/current-id)]
    {:http-xhrio (api/dissoc-job-alerts job-id [alert-id] no-op-ok [rpc-err :rpc/dissoc-job-alerts-err])}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DND
(defn dnd-node
  ([db]
   (get-in db [:jsk.data.explorer.models/dnd-node]))
  ([db new-node]
   (assoc-in db [:jsk.data.explorer.models/dnd-node] new-node)))

(defevent-fx schedule-dnd-drop
  [{:keys [db]} [_ event]]
  (let [node (get-in db [:jsk.data.explorer.models/dnd-node])
        job-id (get-in db m/current-id)]
    ;(log/infof "dropped on schedules area: %s" node)
    (if (= :schedule (:type node))
      {:dispatch [assoc-job-schedule (:jsk-id node)]
       :db (dnd-node db nil)}
      {:db (dnd-node db nil)
       :notify [:alert {:message "Only schedules may be dropped here."}]})))

(defevent-fx alert-dnd-drop
  [{:keys [db]} [_ event]]
  (let [node (get-in db [:jsk.data.explorer.models/dnd-node])
        job-id (get-in db m/current-id)]
    ;(log/infof "dropped on alerts: %s" node)
    (if (= :alert (:type node))
      {:dispatch [assoc-job-alert (:jsk-id node)]
       :db (dnd-node db nil)}
      {:db (dnd-node db nil)
       :notify [:alert {:message "Only alerts may be dropped here."}]})))

(defevent-fx refresh-job
  [{:keys [db]} [_ job-id]]
  (when (= (get-in db m/current-id) job-id)
    {:dispatch [fetch-job job-id]}))
