(ns jsk.data.workflow.events
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [jsk.data.workflow.models :as m]
            [re-frame.core :as rf]
            [jsk.net.api :as api]
            [e85th.ui.util :as u]
            [e85th.ui.rf.plumb :as plumb]
            [e85th.ui.rf.sweet :refer-macros [def-event-db def-event-fx def-db-change]]
            [clojure.string :as str]))

(def-db-change current-name-changed m/current-name)
(def-db-change current-desc-changed m/current-desc)
(def-db-change current-enabled?-changed m/current-enabled?)

(def-event-fx rpc-err
  [{:keys [db] :as cofx} event-v]
  (log/warnf "rpc err: %s" event-v)
  (let [[db msg] (condp = (second event-v)
                   :rpc/fetch-workflow-err [db "Error fetching workflow."]
                   :rpc/save-workflow-err [db "Error saving workflow."]
                   :rpc/assoc-workflow-alerts-err [db "Error associating alert."]
                   :rpc/dissoc-workflow-alerts-err [db "Error dissociating alert."]
                   :rpc/assoc-workflow-schedules-err [db "Error associating schedule."]
                   :rpc/dissoc-workflow-schedules-err [db "Error dissociating schedule."]
                   :else [db "Encountered unhandled RPC error."])]
    {:db (assoc-in db m/busy? false)
     :notify [:alert {:message msg}]}))

(def-event-db no-op-ok
  [db _]
  db)

(def-db-change fetch-workflow-ok m/current)

(def-event-fx fetch-workflow
  [_ [_ workflow-id]]
  {:http-xhrio (api/fetch-workflow workflow-id fetch-workflow-ok [rpc-err :rpc/fetch-workflow-err])})

(def-event-db save-workflow-ok
  [db [_ workflow]]
  (-> db
      (assoc-in m/current workflow)
      (assoc-in m/busy? false)))

(def-event-fx save-workflow
  [{:keys [db]} _]
  (let [workflow (get-in db m/current)]
    {:db (assoc-in db m/busy? true)
     :http-xhrio (api/save-workflow workflow save-workflow-ok [rpc-err :rpc/save-workflow-err])}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schedule Assoc
(def-event-fx assoc-workflow-schedule
  [{:keys [db]} [_ schedule-id]]
  (let [workflow-id (get-in db m/current-id)]
    {:http-xhrio (api/assoc-workflow-schedules workflow-id [schedule-id] no-op-ok [rpc-err :rpc/assoc-workflow-schedules-err])}))

(def-event-fx dissoc-workflow-schedule
  [{:keys [db]} [_ schedule-id]]
  (let [workflow-id (get-in db m/current-id)]
    {:http-xhrio (api/dissoc-workflow-schedules workflow-id [schedule-id] no-op-ok [rpc-err :rpc/dissoc-workflow-schedules-err])}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Alert Assoc
(def-event-fx assoc-workflow-alert
  [{:keys [db]} [_ alert-id]]
  (let [workflow-id (get-in db m/current-id)]
    {:http-xhrio (api/assoc-workflow-alerts workflow-id [alert-id] no-op-ok [rpc-err :rpc/assoc-workflow-alerts-err])}))

(def-event-fx dissoc-workflow-alert
  [{:keys [db]} [_ alert-id]]
  (let [workflow-id (get-in db m/current-id)]
    {:http-xhrio (api/dissoc-workflow-alerts workflow-id [alert-id] no-op-ok [rpc-err :rpc/dissoc-workflow-alerts-err])}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DND
(def-event-db designer-dnd-drop
  [db [_ node-fn]]
  (node-fn (get-in db [:jsk.data.explorer.models/dnd-node]))
  db)

(def-event-fx schedule-dnd-drop
  [{:keys [db]} [_ event]]
  (let [node (get-in db [:jsk.data.explorer.models/dnd-node])
        workflow-id (get-in db m/current-id)]
      ;(log/infof "dropped on schedules area: %s" node)
    (if (= :schedule (:type node))
      {:dispatch [assoc-workflow-schedule (:jsk-id node)]}
      {:notify [:alert {:message "Only schedules may be dropped here."}]})))

(def-event-fx alert-dnd-drop
  [{:keys [db]} [_ event]]
  (let [node (get-in db [:jsk.data.explorer.models/dnd-node])
        workflow-id (get-in db m/current-id)]
      ;(log/infof "dropped on alerts: %s" node)
    (if (= :alert (:type node))
      {:dispatch [assoc-workflow-alert (:jsk-id node)]}
      {:notify [:alert {:message "Only alerts may be dropped here."}]})))


(def-event-fx refresh-workflow
  [{:keys [db]} [_ workflow-id]]
  (when (= (get-in db m/current-id) workflow-id)
    {:dispatch [fetch-workflow workflow-id]}))
