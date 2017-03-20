(ns jsk.data.workflow.events
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [jsk.data.workflow.models :as m]
            [re-frame.core :as rf]
            [jsk.net.api :as api]
            [e85th.ui.util :as u]
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
                   :else [db "Encountered unhandled RPC error."])]
    {:db (assoc-in db m/busy? false)
     :notify [:alert {:message msg}]}))

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
