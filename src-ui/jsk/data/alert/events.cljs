(ns jsk.data.alert.events
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [jsk.data.alert.models :as m]
            [re-frame.core :as rf]
            [jsk.net.api :as api]
            [e85th.ui.util :as u]
            [e85th.ui.rf.sweet :refer-macros [def-event-db def-event-fx def-db-change]]
            [clojure.string :as str]))

(def-db-change name-changed m/current-name)
(def-db-change desc-changed m/current-desc)

(def-event-fx rpc-err
  [{:keys [db] :as cofx} event-v]
  (log/warnf "rpc err: %s" event-v)
  (let [[db msg] (condp = (second event-v)
                   :rpc/fetch-alert-err [db "Error fetching alert."]
                   :rpc/save-alert-err [db "Error saving alert."]
                   :rpc/delete-alert-err [db "Error deleting alert."]
                   :rpc/fetch-alert-list-err [db "Error fetching alert list."]
                   :else [db "Encountered unhandled RPC error."])]
    {:db (assoc-in db m/busy? false)
     :notify [:alert {:message msg}]}))

(def-event-db fetch-alert-list-ok
  [db [_ alert-list]]
  (assoc-in db m/alert-list alert-list))

(def-event-fx fetch-alert-list
  [_ _]
  {:http-xhrio (api/fetch-alert-list fetch-alert-list-ok [rpc-err :rpc/fetch-alert-list-err])})

(def-event-db fetch-alert-ok
  [db [_ alert :as v]]
  (log/infof "current-alert will be: %s" alert)
  (assoc-in db m/current alert))

(def-event-fx fetch-alert
  [_ [_ alert-id]]
  {:http-xhrio (api/fetch-alert alert-id fetch-alert-ok [rpc-err :rpc/fetch-alert-err])})

(def-event-db save-alert-ok
  [db [_ alert]]
   (-> db
       (assoc-in m/current alert)
       (assoc-in m/busy? false)))

(def-event-fx save-alert
  [{:keys [db]} _]
  (let [alert (-> (get-in db m/current)
                  (dissoc :alert/channels-info))]
    {:db (assoc-in db m/busy? true)
     :http-xhrio (api/save-alert alert save-alert-ok [rpc-err :rpc/save-alert-err])}))

(def-event-db new-alert
  [db _]
  (assoc-in db m/current m/new-alert))

(def-event-fx channel-selected
  [db [_ selected-channel]]
  (log/infof "selected-channel: %s" selected-channel)
  {:db db})

(def-event-fx assoc-channel
  [db [_ alert-id chan-id]]
  {:db db})

(def-event-fx dissoc-channel
  [db [_ alert-id chan-id]]
  {:db db})
