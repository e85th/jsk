(ns jsk.test-data
  (:require [clojure.repl :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [e85th.commons.datomic :as datomic]
            [e85th.backend.core.google-oauth :as google-oauth]
            [jsk.data.user :as user]
            [jsk.main :as main]
            [jsk.data.tag :as tag]
            [jsk.data.job :as job]
            [jsk.common.conf :as conf]
            [jsk.data.settings :as settings]
            [jsk.data.db :as db]))

(def user-id 1)

(def user-email "user@foo.com")
(def user-password "123456")

(defn init-settings
  [system]
  (let [{:keys [sys-config]} system]
    (settings/set-google-auth system (conf/google-auth-settings sys-config) user-id)
    (settings/set-smtp system (conf/smtp-settings sys-config) user-id)
    (settings/set-site system (conf/site-settings sys-config) user-id)))

(defn add-users
  [system]
  (user/create-new-user system
                        {:user/first-name "Test"
                         :user/last-name "User"
                         :user/password user-password
                         :user/channels [{:channel/identifier user-email
                                          :channel/type :email}]}
                        user-id))

(defn reset-db!
  [system]
  (init-settings system)
  (add-users system)
  (google-oauth/init! (settings/find-google-auth-client-id system)))
