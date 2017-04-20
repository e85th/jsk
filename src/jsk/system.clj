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
            [jsk.common.artemis :as artemis]
            [jsk.routes :as routes]
            [jsk.publisher :as publisher]
            [jsk.subscriber :as subscriber]
            [jsk.common.conf :as conf]))

(def console-port 9001)
(def nrepl-port 9000)

(def director-inbound-address "jsk.director.inbound")
(def console-inbound-address "jsk.console.inbound")
(def standalone-agent-name "__jsk.agent.standalone__")

(defmulti new-system (fn [mode sys-config] mode))

;; FIXME: refactor common component setup if possible.
(defn console-components
  [sys-config]
  [:sys-config sys-config
   :version "FIXME";(util/build-version)
   :token-factory (token/new-sha256-token-factory (conf/auth-secret sys-config)
                                                  (conf/auth-token-ttl-minutes sys-config))
   :db (datomic/new-datomic-db (conf/datomic-uri sys-config))
   :http (backend-comp/new-http-kit-server {:port console-port} routes/make-handler)
   :ws (backend-ws/new-sente-websocket (sente-http-kit/get-sch-adapter) websockets/req->user-id)
   :console-subscriber (component/using (subscriber/new-console-subscriber console-inbound-address) [:ws])
   :director-publisher (artemis/new-message-producer director-inbound-address)
   :publisher (component/using (publisher/new-web-console-publisher) [:ws :director-publisher])
   :app (component/using (commons-comp/new-app)
                         [:sys-config :version :token-factory :db :publisher :ws])
   :repl (backend-comp/new-repl-server {:port nrepl-port})])

(defn standalone-components
  [sys-config]
  [:sys-config sys-config
   :version "FIXME";(util/build-version)
   :token-factory (token/new-sha256-token-factory (conf/auth-secret sys-config)
                                                  (conf/auth-token-ttl-minutes sys-config))
   :db (datomic/new-datomic-db (conf/datomic-uri sys-config))
   :http (backend-comp/new-http-kit-server {:port console-port} routes/make-handler)
   :ws (backend-ws/new-sente-websocket (sente-http-kit/get-sch-adapter) websockets/req->user-id)

   :mq-server (artemis/new-server)
   :director-publisher (component/using (artemis/new-message-producer director-inbound-address) [:mq-server])
   :publisher (component/using (publisher/new-web-console-publisher) [:ws :director-publisher])
   :agent-publisher (component/using (publisher/new-agent-publisher) [:mq-server])
   :console-publisher (component/using (publisher/new-console-publisher console-inbound-address) [:mq-server])

   :console-subscriber (component/using (subscriber/new-console-subscriber console-inbound-address) [:ws :mq-server])
   :director-subscriber (component/using (subscriber/new-director-subscriber director-inbound-address) [:mq-server :director-resources])
   :agent-subscriber (component/using (subscriber/new-agent-subscriber standalone-agent-name) [:mq-server :agent-resources])
   :agent-resources (component/using (commons-comp/new-app) [:sys-config :version :director-publisher])
   :director-resources (component/using (commons-comp/new-app) [:sys-config :version :agent-publisher :console-publisher :db])
   :app (component/using (commons-comp/new-app) [:sys-config :version :token-factory :db :publisher :ws])
   :repl (backend-comp/new-repl-server {:port nrepl-port})])

(defmethod new-system :console
  [_ sys-config]
  (apply component/system-map (console-components sys-config)))

(defmethod new-system :conductor
  [_ sys-config])

(defmethod new-system :agent
  [_ sys-config])

(defmethod new-system :standalone
  [_ sys-config]
  (apply component/system-map (standalone-components sys-config)))
