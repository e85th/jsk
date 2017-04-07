(ns jsk.login.events
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [jsk.common.data :as data]
            [jsk.net.api :as api]
            [jsk.websockets :as websockets]
            [jsk.login.models :as m]
            [jsk.models :as jsk-models]
            [e85th.ui.rf.macros :refer-macros [defevent-db defevent-fx defevent]]
            [e85th.ui.util :as u]
            [e85th.ui.browser :as browser]
            [re-frame.core :as rf]))


(defevent email-changed m/email)
(defevent password-changed m/password)

(def auth-err ::auth-err)

(defevent-fx rpc-err
  [{:keys [db] :as cofx} event-v]
  (log/warnf "rpc err: %s" event-v)
  (let [[db msg] (condp = (second event-v)
                   auth-err [(assoc-in db m/login-busy? false) "Error authenticating."]
                   :else [db "Encountered unhandled RPC error."])]
    {:db db
     :notify [:alert {:message msg}]}))

(defevent-fx logout
  [_ _]
  {:local-storage {:clear true}
   :db {}
   :nav "/"})

(defn clear-login-details
  [db]
  (-> db
      (assoc-in m/email "")
      (assoc-in m/password "")))

(defevent-fx auth-ok
  [{:keys [db] :as cofx} [_ {:keys [user token roles] :as user-info}]]
  (let [roles (set (map keyword roles))]
    (data/set-jsk-token! token) ;; in local storage so will persist
    {:db (assoc-in db jsk-models/current-user user)
     :nav "/"}))

(defevent-fx google-auth
  [_ [_ token]]
  (log/infof "auth called with token: %s" token)
  {:http-xhrio (api/authenticate-with-google token auth-ok [rpc-err auth-err])})

(defevent-fx email-pass-auth
  [{:keys [db]} _]
  (let [email (get-in db m/email)
        password (get-in db m/password)]
    {:db (assoc-in db m/login-busy? true)
     :http-xhrio (api/authenticate-with-password email password auth-ok [rpc-err auth-err])}))
