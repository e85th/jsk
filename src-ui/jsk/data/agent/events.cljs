(ns jsk.data.agent.events
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [jsk.data.agent.models :as m]
            [re-frame.core :as rf]
            [jsk.net.api :as api]
            [e85th.ui.util :as u]
            [e85th.ui.rf.macros :refer-macros [defevent-db defevent-fx defevent]]
            [clojure.string :as str]))

(defevent name-changed m/current-name)

(defevent-fx rpc-err
  [{:keys [db] :as cofx} event-v]
  (log/warnf "rpc err: %s" event-v)
  (let [[db msg] (condp = (second event-v)
                   :rpc/fetch-agent-err [db "Error fetching agent."]
                   :rpc/save-agent-err [db "Error saving agent."]
                   :else [db "Encountered unhandled RPC error."])]
    {:db (assoc-in db m/busy? false)
     :notify [:alert {:message msg}]}))

(defevent fetch-agent-ok m/current)

(defevent-fx fetch-agent
  [_ [_ agent-id]]
  {:http-xhrio (api/fetch-agent agent-id fetch-agent-ok [rpc-err :rpc/fetch-agent-err])})

(defevent-db save-agent-ok
  [db [_ agent]]
   (-> db
       (assoc-in m/current agent)
       (assoc-in m/busy? false)))

(defevent-fx save-agent
  [{:keys [db]} _]
  (let [agent (get-in db m/current)]
    {:db (assoc-in db m/busy? true)
     :http-xhrio (api/save-agent agent save-agent-ok [rpc-err :rpc/save-agent-err])}))

(defevent-fx refresh-agent
  [{:keys [db]} [_ agent-id]]
  (when (= (get-in db m/current-id) agent-id)
    {:dispatch [fetch-agent agent-id]}))
