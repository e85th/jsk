(ns jsk.events
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [jsk.common.data :as data]
            [jsk.net.api :as api]
            [jsk.websockets :as websockets]
            [jsk.models :as m]
            [e85th.ui.rf.sweet :refer-macros [def-event-db def-event-fx def-db-change]]
            [e85th.ui.util :as u]
            [re-frame.core :as rf]))

(def auth-err ::auth-err)

(def-event-fx rpc-err
  [{:keys [db] :as cofx} event-v]
  (log/warnf "rpc err: %s" event-v)
  (let [[db msg] (condp = (second event-v)
                   auth-err [db "Error authenticating."]
                   :else [db "Encountered unhandled RPC error."])]
    {:db db
     :notify [:alert {:message msg}]}))


(def-db-change set-current-view m/current-view)

(def-event-fx logout
  [_ _]
  {:local-storage {:clear true}
   :nav (data/login-url)})

(def-event-fx auth-ok
  [{:keys [db] :as cofx} [_ {:keys [user token roles] :as user-info}]]
  (let [roles (set (map keyword roles))]
    (data/set-jsk-token! token) ;; in local storage so will persist
    (u/set-cookie "jsk-token" token -1 nil)
    {:db (assoc-in db m/current-user user)
     :nav "/"}))

(def-event-fx google-auth
  [_ [_ token]]
  (log/infof "auth called with token: %s" token)
  {:http-xhrio (api/google-authenticate token auth-ok [rpc-err auth-err])})
