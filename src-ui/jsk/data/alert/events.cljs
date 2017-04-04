(ns jsk.data.alert.events
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [jsk.data.alert.models :as m]
            [re-frame.core :as rf]
            [jsk.net.api :as api]
            [e85th.ui.util :as u]
            [e85th.ui.rf.macros :refer-macros [defevent-db defevent-fx defevent]]
            [clojure.string :as str]))

(defevent name-changed m/current-name)
(defevent desc-changed m/current-desc)

(defevent-fx rpc-err
  [{:keys [db] :as cofx} event-v]
  (log/warnf "rpc err: %s" event-v)
  (let [[db msg] (condp = (second event-v)
                   :rpc/fetch-alert-err [db "Error fetching alert."]
                   :rpc/save-alert-err [db "Error saving alert."]
                   :rpc/fetch-channel-suggestions-err [db "Error fetching channel suggestions."]
                   :rpc/assoc-alert-channel-err [db "Error associating channel to alert."]
                   :rpc/dissoc-alert-channel-err [db "Error dissociating channel from alert."]
                   :else [db "Encountered unhandled RPC error."])]
    {:db (assoc-in db m/busy? false)
     :notify [:alert {:message msg}]}))

(defevent-db no-op-event
  [db _] db)

(defevent fetch-alert-ok m/current)

(defevent-fx fetch-alert
  [_ [_ alert-id]]
  {:http-xhrio (api/fetch-alert alert-id fetch-alert-ok [rpc-err :rpc/fetch-alert-err])})

(defevent-db save-alert-ok
  [db [_ alert]]
   (-> db
       (assoc-in m/current alert)
       (assoc-in m/busy? false)))

(defevent-fx save-alert
  [{:keys [db]} _]
  (let [alert (-> (get-in db m/current)
                  (dissoc :alert/channels-info))]
    {:db (assoc-in db m/busy? true)
     :http-xhrio (api/save-alert alert save-alert-ok [rpc-err :rpc/save-alert-err])}))

(defevent-fx dissoc-alert-channel
  [{:keys [db]} [_ channel-id]]
  (let [alert-id (get-in db m/current-id)]
    {:http-xhrio (api/dissoc-alert-channels alert-id
                                            [channel-id]
                                            no-op-event
                                            [rpc-err :rpc/dissoc-alert-channel-err])}))


(defevent-db channel-suggestions-fetch-ok
  [db [_ items]]
  (assoc-in db m/channel-suggestions items))

(defevent-fx channel-suggestion-text-changed
  [_ [_ text]]
  {:http-xhrio (api/suggest-channels text channel-suggestions-fetch-ok [rpc-err :rpc/fetch-channel-suggestions-err])})



(defevent-fx channel-suggestion-selected
  [{:keys [db]} [_ [channel-id :as selected-channel]]]
  ;(log/infof "selected-channel: %s, current-alert: %s" selected-channel (get-in db m/current))
  (let [alert-id (get-in db m/current-id)]
    {:http-xhrio (api/assoc-alert-channels alert-id
                                           [channel-id]
                                           no-op-event
                                           [rpc-err :rpc/assoc-alert-channel-err])}))


(defevent-fx refresh-alert
  [{:keys [db]} [_ alert-id]]
  (when (= (get-in db m/current-id) alert-id)
    {:dispatch [fetch-alert alert-id]}))
