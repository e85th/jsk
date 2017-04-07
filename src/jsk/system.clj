(ns jsk.system
  (:require [com.stuartsierra.component :as component]
            [e85th.backend.components :as backend-comp]
            [e85th.commons.components :as commons-comp]
            [e85th.commons.email :as email]
            [e85th.commons.token :as token]
            [e85th.commons.datomic :as datomic]
            [e85th.backend.websockets :as backend-ws]
            [taoensso.sente.server-adapters.http-kit :as sente-http-kit]
            [jsk.websockets :as websockets]
            [schema.core :as s]
            [jsk.common.util :as util]
            [jsk.routes :as routes]
            [jsk.publisher :as publisher]
            [jsk.common.conf :as conf]))

(def console-port 9001)
(def nrepl-port 9000)

(defmulti new-system (fn [mode sys-config] mode))

(defn console-components
  [sys-config]
  [:sys-config sys-config
   :version "FIXME";(util/build-version)
   :token-factory (token/new-sha256-token-factory (conf/auth-secret sys-config)
                                                  (conf/auth-token-ttl-minutes sys-config))
   :db (datomic/new-datomic-db (conf/datomic-uri sys-config))
   :http (backend-comp/new-http-kit-server {:port console-port} routes/make-handler)
   :ws (backend-ws/new-sente-websocket (sente-http-kit/get-sch-adapter) websockets/req->user-id)
   :publisher (component/using (publisher/new-web-server-publisher) [:ws])
   :repl (backend-comp/new-repl-server {:port nrepl-port})
   :app (component/using (commons-comp/new-app) [:sys-config :version :token-factory :db :publisher :ws])])

(defmethod new-system :console
  [_ sys-config]
  (apply component/system-map (console-components sys-config)))

(defmethod new-system :conductor
  [_ sys-config])

(defmethod new-system :agent
  [_ sys-config])

(defmethod new-system :standalone
  [_ sys-config]
  (apply component/system-map (console-components sys-config)))
