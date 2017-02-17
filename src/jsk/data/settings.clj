(ns jsk.data.settings
  (:require [schema.core :as s]
            [jsk.data.models :as m]
            [datomic.api :as d]
            [taoensso.timbre :as log]))

(s/defn find-settings
  [{:keys [db]} attr :- s/Keyword]
  (ffirst (d/q '[:find (pull ?eid [*] ...)
                 :in $ ?attr
                 :where [?eid ?attr]]
               (-> db :cn d/db)
               attr)))

(s/defn find-google-auth :- (s/maybe m/GoogleAuthSettings)
  [res]
  (find-settings res :setting.auth.google/client-id))

(s/defn find-google-auth-client-id :- (s/maybe s/Str)
  [res]
  (:setting.auth.google/client-id (find-google-auth res)))

(s/defn set-google-auth :- m/GoogleAuthSettings
  "Saves the settings."
  [{:keys [db] :as res} auth :- m/GoogleAuthSettings user-id :- s/Int]
  @(d/transact (:cn db) [(merge (find-google-auth res) auth)])
  (find-google-auth res))

(s/defn find-smtp :- (s/maybe m/SmtpSettings)
  [res]
  (find-settings res :setting.smtp/host))

(s/defn set-smtp :- m/SmtpSettings
  [{:keys [db] :as res} smtp :- m/SmtpSettings user-id :- s/Int]
  @(d/transact (:cn db) [(merge (find-smtp res) smtp)])
  (find-smtp res))

(s/defn find-slack :- (s/maybe m/SlackSettings)
  [res]
  (find-settings res :setting.slack/token))

(s/defn set-slack :- m/SlackSettings
  [{:keys [db] :as res} slack :- m/SlackSettings user-id :- s/Int]
  @(d/transact (:cn db) [(merge (find-slack res) slack)])
  (find-slack res))

(s/defn find-site :- (s/maybe m/SiteSettings)
  [res]
  (find-settings res :setting.site/name))

(s/defn set-site :- m/SiteSettings
  [{:keys [db] :as res} site :- m/SiteSettings user-id :- s/Int]
  @(d/transact (:cn db) [(merge (find-site res) site)])
  (find-site res))
