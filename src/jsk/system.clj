(ns jsk.system
  (:require [com.stuartsierra.component :as component]
            [e85th.backend.components :as backend-comp]
            [e85th.commons.components :as commons-comp]
            [e85th.commons.aws.ses :as ses]
            [e85th.commons.aws.sns :as sns]
            [e85th.commons.email :as email]
            [e85th.commons.token :as token]
            [e85th.commons.datomic :as datomic]
            [e85th.backend.websockets :as backend-ws]
            [taoensso.sente.server-adapters.http-kit :as sente-http-kit]
            [jsk.websockets :as websockets]
            [schema.core :as s]
            [jsk.common.util :as util]
            [jsk.routes :as routes]
            [jsk.common.conf :as conf]))



(s/defn add-server-components
  "Adds server components "
  [sys-config component-vector]
  (let [component-vector (conj component-vector :ws (backend-ws/new-sente-websocket (sente-http-kit/get-sch-adapter) websockets/req->user-id))]
    (conj component-vector
          ;; app will have all dependent resources
          :app (-> component-vector commons-comp/component-keys commons-comp/new-app)
          :http (backend-comp/new-http-kit-server {:port 9001} routes/make-handler)

          :repl (backend-comp/new-repl-server {:port 9000}))))

(s/defn all-components
  "Answers with a seq of alternating keywords and components required for component/system-map.
   Starts with a base set of components and adds in other components based on the operation mode."
  [sys-config operation-mode :- s/Keyword]
  (let [base [:sys-config sys-config
              :mailer (ses/new-ses-email-sender)
              :version "FIXME";(util/build-version)
              :token-factory (token/new-sha256-token-factory (conf/auth-secret sys-config)
                                                             (conf/auth-token-ttl-minutes sys-config))
              :sms (sns/new-sms-sender)
              :db (datomic/new-datomic-db (conf/datomic-uri sys-config))]
        f (get {:server add-server-components
                :standalone add-server-components}
               operation-mode
               (constantly base))]
    (f sys-config base)))

(s/defn new-system
  "Creates a system."
  [sys-config operation-mode :- s/Keyword]
  (apply component/system-map (all-components sys-config operation-mode)))
