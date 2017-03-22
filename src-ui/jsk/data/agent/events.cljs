(ns jsk.data.agent.events
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [jsk.data.agent.models :as m]
            [re-frame.core :as rf]
            [jsk.net.api :as api]
            [e85th.ui.util :as u]
            [e85th.ui.rf.sweet :refer-macros [def-event-db def-event-fx def-db-change]]
            [clojure.string :as str]))

(def-db-change name-changed m/current-name)

(def-event-fx rpc-err
  [{:keys [db] :as cofx} event-v]
  (log/warnf "rpc err: %s" event-v)
  (let [[db msg] (condp = (second event-v)
                   :rpc/fetch-agent-err [db "Error fetching agent."]
                   :rpc/save-agent-err [db "Error saving agent."]
                   :else [db "Encountered unhandled RPC error."])]
    {:db (assoc-in db m/busy? false)
     :notify [:alert {:message msg}]}))

(def-db-change fetch-agent-ok m/current)

(def-event-fx fetch-agent
  [_ [_ agent-id]]
  {:http-xhrio (api/fetch-agent agent-id fetch-agent-ok [rpc-err :rpc/fetch-agent-err])})

(def-event-db save-agent-ok
  [db [_ agent]]
   (-> db
       (assoc-in m/current agent)
       (assoc-in m/busy? false)))

(def-event-fx save-agent
  [{:keys [db]} _]
  (let [agent (get-in db m/current)]
    {:db (assoc-in db m/busy? true)
     :http-xhrio (api/save-agent agent save-agent-ok [rpc-err :rpc/save-agent-err])}))

(def-event-fx refresh-agent
  [{:keys [db]} [_ agent-id]]
  (when (= (get-in db m/current-id) agent-id)
    {:dispatch [fetch-agent agent-id]}))
