(ns jsk.data.explorer.events
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [jsk.common.data :as data]
            [jsk.net.api :as api]
            [jsk.data.explorer.models :as m]
            [e85th.ui.rf.sweet :refer-macros [def-event-db def-event-fx def-db-change]]
            [e85th.ui.util :as u]
            [e85th.ui.rf.tree :as tree]
            [re-frame.core :as rf]))

(def executables-id "jsk/executables")
(def alerts-id "jsk/alerts")
(def agents-id "jsk/agents")
(def schedules-id "jsk/schedules")

(def ^:private tree-sections
  [{:id agents-id
    :parent "#"
    :text "Agents"
    :jsk-node-type :agent
    :jsk-id agents-id
    :type :section
    :children true}
   {:id alerts-id
    :parent "#"
    :text "Alerts"
    :jsk-node-type :alert
    :jsk-id alerts-id
    :type :section
    :children true}
   {:id executables-id
    :parent "#"
    :text "Executables"
    :jsk-node-type :executable
    :jsk-id executables-id
    :type :section
    :children true}
   {:id schedules-id
    :parent "#"
    :text "Schedules"
    :jsk-node-type :schedule
    :jsk-id schedules-id
    :type :section
    :children true}])

(def-db-change set-explorer-view m/explorer-view)
(def-db-change set-dnd-node m/dnd-node)

(def-event-fx rpc-err
  [{:keys [db] :as cofx} event-v]
  (log/warnf "rpc err: %s" event-v)
  (let [[db msg] (condp = (second event-v)
                   :rpc/fetch-children-err [db "Error fetching explorer children."]
                   :rpc/create-explorer-node-err [db "Error adding explorer node."]
                   :rpc/rm-explorer-node-err [db "Error removing explorer node."]
                   :else [db "Encountered unhandled RPC error."])]
    {:db db
     :notify [:alert {:message msg}]}))

(defn set-tree-data
  [cb data]
  (cb (clj->js data)))

(def-event-fx fetch-children-ok
  [_ [_ parent-node cb data]]
  (let [{parent-id :id} parent-node
        as-node (fn [{node-id :node/id node-name :node/name node-type :node/type}]
                  ;; id for sql dbs may not be unique
                  {:id (str (name node-type) "-" node-id)
                   :parent parent-id
                   :text node-name
                   :type node-type
                   :jsk-node-type node-type
                   :jsk-id node-id})]
    (->> (map as-node data)
         (set-tree-data cb))))

(def-event-fx fetch-children
  [{:keys [db]} [_ node cb]]
  ;(log/infof "node: %s" node)
  ;; node will be nil if the fetch-children happened on the root node
  (if (or (nil? node) (tree/root? node))
    (set-tree-data cb tree-sections)
    {:http-xhrio (api/fetch-explorer-nodes
                  (:jsk-node-type node)
                  [fetch-children-ok node cb] [rpc-err :rpc/fetch-children-err])}))

(defn move-disallowed?
  [{:keys [parent old-parent node] :as data}]
  true
  #_(or (tree/root-id? old-parent)
      (not= (jsk-node-type (tree/node m/tree-sel parent))
            (jsk-node-type (tree/node m/tree-sel old-parent)))))

(def-event-fx node-moved
  [_ [_ {:keys [parent old-parent node] :as data}]]
  (if (move-disallowed? data)
    (do
      (tree/refresh m/tree-sel)
      {:notify [:alert {:message "Sorry. That's not supported."}]})
    {}))

(def-event-db no-op-ok
  [db _]
  db)

(def-event-fx create-explorer-node
  [_ [_ type]]
  {:http-xhrio (api/create-explorer-node (name type) no-op-ok [rpc-err :rpc/create-explorer-node-err])})


(def-event-fx rm-explorer-node
  [_ [_ type id]]
  {:http-xhrio (api/rm-explorer-node (name type) id no-op-ok [rpc-err :rpc/rm-explorer-node-err])})

(def-event-fx trigger-job
  [_ _]
  {:notify [:alert {:message "TODO: Trigger job"}]})

(def-event-fx trigger-workflow
  [_ _]
  {:notify [:alert {:message "TODO: Trigger workflow"}]})

(def-event-fx refresh-node
  [_ [_ node-id]]
  (tree/trigger-load m/tree-sel node-id)
  {})
