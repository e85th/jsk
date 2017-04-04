(ns jsk.data.schedule.events
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [jsk.data.schedule.models :as m]
            [re-frame.core :as rf]
            [jsk.net.api :as api]
            [e85th.ui.util :as u]
            [e85th.ui.rf.macros :refer-macros [defevent-db defevent-fx defevent]]
            [clojure.string :as str]))

(defevent current-name-changed m/current-name)
(defevent current-desc-changed m/current-desc)
(defevent current-cron-expr-changed m/current-cron-expr)

(defevent-fx rpc-err
  [{:keys [db] :as cofx} event-v]
  (let [[db msg] (condp = (second event-v)
                   :rpc/fetch-schedule-err [db "Error fetching schedule."]
                   :rpc/save-schedule-err [db "Error saving schedule."]
                   :else [db "Encountered unhandled RPC error."])]
    {:db (assoc-in db m/busy? false)
     :notify [:alert {:message msg}]}))

(defevent fetch-schedule-ok m/current)

(defevent-fx fetch-schedule
  [_ [_ schedule-id]]
  {:http-xhrio (api/fetch-schedule schedule-id fetch-schedule-ok [rpc-err :rpc/fetch-schedule-err])})

(defevent-db save-schedule-ok
  [db [_ schedule]]
   (-> db
       (assoc-in m/current schedule)
       (assoc-in m/busy? false)))

(defevent-fx save-schedule
  [{:keys [db]} _]
  (let [schedule (get-in db m/current)]
    {:db (assoc-in db m/busy? true)
     :http-xhrio (api/save-schedule schedule save-schedule-ok [rpc-err :rpc/save-schedule-err])}))

(defevent-fx refresh-schedule
  [{:keys [db]} [_ schedule-id]]
  (when (= (get-in db m/current-id) schedule-id)
    {:dispatch [fetch-schedule schedule-id]}))
