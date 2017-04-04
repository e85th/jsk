(ns jsk.data.workflow.events
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [jsk.data.workflow.models :as m]
            [re-frame.core :as rf]
            [jsk.net.api :as api]
            [jsk.data.workflow.designer :as designer]
            [e85th.ui.util :as u]
            [e85th.ui.rf.plumb :as plumb]
            [e85th.ui.rf.macros :refer-macros [defevent-db defevent-fx defevent]]
            [clojure.string :as str]))

(defevent current-name-changed m/current-name)
(defevent current-desc-changed m/current-desc)
(defevent current-enabled?-changed m/current-enabled?)
(defevent set-plumb-instance m/plumb-instance)

(defevent-fx rpc-err
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

(defevent-db no-op-ok [db _] db)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schedule Assoc
(defevent-fx assoc-workflow-schedule
  [{:keys [db]} [_ schedule-id]]
  (let [workflow-id (get-in db m/current-id)]
    {:http-xhrio (api/assoc-workflow-schedules workflow-id [schedule-id] no-op-ok [rpc-err :rpc/assoc-workflow-schedules-err])}))

(defevent-fx dissoc-workflow-schedule
  [{:keys [db]} [_ schedule-id]]
  (let [workflow-id (get-in db m/current-id)]
    {:http-xhrio (api/dissoc-workflow-schedules workflow-id [schedule-id] no-op-ok [rpc-err :rpc/dissoc-workflow-schedules-err])}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Alert Assoc
(defevent-fx assoc-workflow-alert
  [{:keys [db]} [_ alert-id]]
  (let [workflow-id (get-in db m/current-id)]
    {:http-xhrio (api/assoc-workflow-alerts workflow-id [alert-id] no-op-ok [rpc-err :rpc/assoc-workflow-alerts-err])}))

(defevent-fx dissoc-workflow-alert
  [{:keys [db]} [_ alert-id]]
  (let [workflow-id (get-in db m/current-id)]
    {:http-xhrio (api/dissoc-workflow-alerts workflow-id [alert-id] no-op-ok [rpc-err :rpc/dissoc-workflow-alerts-err])}))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DND
(defn dnd-node
  ([db]
   (get-in db [:jsk.data.explorer.models/dnd-node]))
  ([db new-node]
   (assoc-in db [:jsk.data.explorer.models/dnd-node] new-node)))

(defevent-fx schedule-dnd-drop
  [{:keys [db]} [_ event]]
  (if-let [node (dnd-node db)]
    (let [workflow-id (get-in db m/current-id)]
      (if (= :schedule (:type node))
        {:dispatch [assoc-workflow-schedule (:jsk-id node)]
         :db (dnd-node db nil)}
        {:db (dnd-node db nil)
         :notify [:alert {:message "Only schedules may be dropped here."}]}))
    {}))

(defevent-fx alert-dnd-drop
  [{:keys [db]} [_ event]]
  (if-let [node (dnd-node db)]
    (let [workflow-id (get-in db m/current-id)]
      (if (= :alert (:type node))
        {:dispatch [assoc-workflow-alert (:jsk-id node)]
         :db (dnd-node db nil)}
        {:db (dnd-node db nil)
         :notify [:alert {:message "Only alerts may be dropped here."}]}))
    {}))

(defevent-fx workflow-node-removed
  [{:keys [db]} [_ dom-id]]
  (let [pb (get-in db m/plumb-instance)]
    (plumb/rm pb dom-id);; remove from dom
    {:db (update-in db m/graph dissoc dom-id)}))

(defevent-fx designer-dnd-drop
  [{:keys [db]} [_ coords]]
  ;; check for node because random drag and drop events can originate
  ;; from the designer itself (highlight with mouse)
  ;; unset after it is handled
  (if-let [node (dnd-node db)]
    (if (#{:job :workflow} (:type node))
      (let [pb (get-in db m/plumb-instance)
            {:keys [dom-id]} (designer/add-workflow-node pb (:text node) coords workflow-node-removed)]
        (dnd-node db nil)
        {:db (update-in db m/graph assoc dom-id (m/graph-node (:jsk-id node)))})
      {:db (dnd-node db nil)
       :notify [:alert {:message "Only jobs and schedules may be dropped here."}]})
    {}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Connections created and removed
(defn- update-graph
  "Update the data in the db. cn is a jsplumb connection object.
   Adds or removes an edge."
  [graph-mod-fn db cn]
  (let [{:keys [source-id target-id]} (plumb/connection-as-map cn) ; these are dom-ids
        {:keys [node-id kind]} (designer/decons-source-id source-id) ; node-id is the workflow node's dom-id
        graph (get-in db m/graph)
        db-target-id (m/graph-dom-id->db-id graph target-id)
        graph* (graph-mod-fn graph node-id db-target-id kind)]
    ;(log/infof "graph before: %s" graph)
    ;(log/infof "graph after: %s" graph*)
    (assoc-in db m/graph graph*)))

(defevent-fx detach-connection
  [{:keys [db]} [_ cn]]
  (let [pb (get-in db m/plumb-instance)]
    (plumb/detach-connection pb cn)
    {:db (update-graph m/graph-rm-edge db cn)}))

(defevent-fx connection-created
  [{:keys [db]} [_ cn]]
  {:db (update-graph m/graph-add-edge db cn)})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workflow Fetch
(defn build-graph
  [nodes]
  )

(defevent-fx fetch-workflow-ok
  [{:keys [db]} [_ workflow]]
  (let [pb (get-in db m/plumb-instance)
        {:keys [workflow/nodes]} workflow]
    (designer/populate pb nodes workflow-node-removed)
    {:db (-> (assoc-in db m/current workflow)
             (assoc-in m/graph (build-graph nodes)))}))

(defevent-fx fetch-workflow
  [_ [_ workflow-id]]
  {:http-xhrio (api/fetch-workflow workflow-id fetch-workflow-ok [rpc-err :rpc/fetch-workflow-err])})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workflow Save
(defevent-db save-workflow-ok
  [db [_ workflow]]
  (-> db
      (assoc-in m/current workflow)
      (assoc-in m/busy? false)))

(defn graph->workflow-nodes
  [g]
  (map (fn [{:keys [id ok err]}]
         {:workflow.node/item id
          :workflow.node/successors (vec ok)
          :workflow.node/successors-err (vec err)})
       (vals g)))

(defevent-fx save-workflow
  [{:keys [db]} _]
  (let [workflow (get-in db m/current)
        graph (get-in db m/graph)
        workflow (assoc workflow :workflow/nodes (graph->workflow-nodes graph))]
    (log/infof "graph: %s" graph)
    ;(log/infof "workflow: %s" workflow)
    {:db (assoc-in db m/busy? true)
     :http-xhrio (api/save-workflow workflow save-workflow-ok [rpc-err :rpc/save-workflow-err])}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Refresh
(defevent-fx refresh-workflow
  [{:keys [db]} [_ workflow-id]]
  (when (= (get-in db m/current-id) workflow-id)
    {:dispatch [fetch-workflow workflow-id]}))
