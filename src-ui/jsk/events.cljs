(ns jsk.events
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [jsk.common.data :as data]
            [jsk.net.api :as api]
            [jsk.websockets :as websockets]
            [jsk.models :as m]
            [e85th.ui.rf.macros :refer-macros [defevent defevent-db defevent-fx]]
            [e85th.ui.util :as u]
            [re-frame.core :as rf]))

(defevent set-main-view m/main-view)

(defevent-fx rpc-err
  [{:keys [db] :as cofx} event-v]
  (log/warnf "rpc err: %s" event-v)
  (let [[db msg] (condp = (second event-v)
                   :jsk/fetch-user-err [db "Error fetching user details."]
                   :else [db "Encountered unhandled RPC error."])]
    {:db db
     :notify [:alert {:message msg}]}))

(defevent-db fetch-user-ok
  [db [_ user]]
  (assoc-in db m/current-user user))

(defevent-fx fetch-user
  [_ _]
  {:http-xhrio (api/fetch-current-user fetch-user-ok [rpc-err :jsk/fetch-user-err])})
