(ns jsk.test-system
  (:require [com.stuartsierra.component :as component]
            [jsk.common.conf :as conf]
            [e85th.commons.util :as u]
            [e85th.commons.sql :as sql]
            [e85th.commons.email :as email]
            [e85th.commons.sms :as sms]
            [e85th.commons.components :as commons-comp]
            [schema.core :as s]
            [taoensso.timbre :as log]))

(s/defn add-server-components
  "Adds server components "
  [sys-config component-vector]
  (conj component-vector
        ;; app will have all dependent resources
        :app (-> component-vector commons-comp/component-keys commons-comp/new-app)))

(s/defn all-components
  "Answers with a seq of alternating keywords and components required for component/system-map.
   Starts with a base set of components and adds in server components."
  [sys-config]
  (let [base [:sys-config sys-config
              :mailer (email/new-nil-email-sender)
              :sms (sms/new-nil-sms-sender)
              :db (sql/new-connection-pool (conf/db-spec sys-config))]]
    (add-server-components sys-config base)))

(s/defn make
  "Creates a system."
  [sys-config]
  (apply component/system-map (all-components sys-config)))
