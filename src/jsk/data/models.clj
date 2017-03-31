(ns jsk.data.models
  (:require [schema.core :as s]
            [e85th.commons.util :as u]
            [clojure.set :as set])
  (:import [org.joda.time DateTime]))

(def standard-job-behavior :standard)
(def standard-job-behavior-id 1)
(def loop-job-behavior :loop)
(def loop-job-behavior-id 2)
(def cond-job-behavior :cond)
(def cond-job-behavior-id 3)

(def job-behavior->id
  {standard-job-behavior standard-job-behavior-id
   loop-job-behavior loop-job-behavior-id
   cond-job-behavior cond-job-behavior-id})

(def behavior-id->name
  (set/map-invert job-behavior->id))

(s/defschema Channel
  {:channel/identifier s/Str
   :channel/type s/Keyword
   (s/optional-key :channel/token) s/Str
   (s/optional-key :channel/token-expiration) DateTime
   s/Keyword s/Any})

(s/defschema Permission
  {(s/optional-key :db/id) s/Int
   :permission/name s/Keyword
   :permission/desc s/Str})

(s/defschema Role
  {(s/optional-key :db/id) s/Int
   :role/name s/Keyword
   :role/desc s/Str
   :role/permissions [Permission]})

(s/defschema User
  {(s/optional-key :db/id) s/Int
   :user/first-name s/Str
   :user/last-name s/Str
   :user/channels [Channel]
   (s/optional-key :user/password) s/Str
   (s/optional-key :user/roles) [Role]
   s/Keyword s/Any})

(s/defschema TokenUser
  {:db/id s/Int
   :user/first-name s/Str
   :user/last-name s/Str})

(s/defschema UserAllFields
  (assoc User :user/password-hash s/Str))

(s/defschema OneTimePassRequest
  {:identifier s/Str
   :token s/Str
   :token-expiration DateTime
   :message-body s/Str})

(s/defschema AuthResponse
  {:user User
   :roles #{s/Keyword}
   :token s/Str})

;; -- Agent
(s/defschema Agent
  {(s/optional-key :db/id) s/Int
   :agent/name s/Str})

(defn new-agent
  []
  {:agent/name (u/uuid)})

;; -- Alert
(s/defschema Alert
  {(s/optional-key :db/id) s/Int
   :alert/name s/Str
   (s/optional-key :alert/desc) s/Str
   (s/optional-key :alert/channels) [s/Int]})

(defn new-alert
  []
  {:alert/name (u/uuid)
   :alert/desc ""})

(s/defschema AlertChannels
  {:alert/id s/Int
   :channel/ids [s/Int]})

(s/defschema ChannelInfo
  {:channel/id s/Int
   :channel/identifier s/Str
   :user/first-name s/Str
   :user/last-name s/Str})

(s/defschema AlertInfo
  (-> Alert
      (dissoc :alert/channels)
      (assoc :alert/channels-info [ChannelInfo])))

;; -- Schedule
(s/defschema Schedule
  {(s/optional-key :db/id) s/Int
   :schedule/name s/Str
   (s/optional-key :schedule/desc) s/Str
   :schedule/cron-expr s/Str})

(defn new-schedule
  []
  {:schedule/name (u/uuid)
   :schedule/desc ""
   :schedule/cron-expr  "1 1 1 1 1 ? 2100"})

;; -- Tag
(s/defschema Tag
  {(s/optional-key :db/id) s/Int
   :tag/name s/Str})

;; -- Job
(s/defschema Job
  {(s/optional-key :db/id) s/Int
   :job/name s/Str
   (s/optional-key :job/desc) s/Str
   :job/enabled? s/Bool
   (s/optional-key :job/agent) (s/maybe s/Int)
   :job/type s/Keyword
   :job/props {s/Keyword s/Any}
   :job/behavior s/Keyword
   (s/optional-key :job/context-key) (s/maybe s/Str)
   :job/max-retries s/Int
   :job/max-concurrent s/Int
   :job/timeout-ms s/Int
   (s/optional-key :job/alerts) [s/Int]
   (s/optional-key :job/schedules) [s/Int]
   (s/optional-key :job/tags) [s/Int]})

(s/defschema JobInfo
  (-> Job
      (dissoc (s/optional-key :job/alerts) (s/optional-key :job/schedules) (s/optional-key :job/tags))
      (assoc :job/schedules [Schedule] :job/alerts [Alert] :job/tags [Tag])))

(s/defschema JobSchedules
  {:job/id s/Int
   :schedule/ids [s/Int]})

(s/defschema JobAlerts
  {:job/id s/Int
   :alert/ids [s/Int]})

(s/defschema JobTags
  {:job/id s/Int
   :tag/ids [s/Int]})

(defn new-job
  []
  {:job/name (u/uuid)
   :job/enabled? true
   :job/type :shell
   :job/props {}
   :job/behavior standard-job-behavior
   :job/max-retries 1
   :job/max-concurrent 1
   :job/timeout-ms -1})

(s/defschema JobField
  {:job.field/name s/Str
   :job.field/desc s/Str
   :job.field/key s/Keyword
   :job.field/type s/Keyword
   :job.field/required? s/Bool})

(s/defschema JobTypeSchema
  {s/Keyword [JobField]})

(s/defschema WorkflowNode
  {(s/optional-key :db/id) s/Int
   :workflow.node/item s/Int
   (s/optional-key :workflow.node/successors) [s/Int]
   (s/optional-key :workflow.node/successors-err) [s/Int]})

(s/defschema WorkflowNodeInfo
  (assoc WorkflowNode
         (s/optional-key :job/id) s/Int
         (s/optional-key :job/name) s/Str
         (s/optional-key :workflow/id) s/Int
         (s/optional-key :workflow/name) s/Str))


;; -- Workflow
(s/defschema Workflow
  {(s/optional-key :db/id) s/Int
   :workflow/name s/Str
   :workflow/desc (s/maybe s/Str)
   :workflow/enabled? s/Bool
   (s/optional-key :workflow/alerts) [s/Int]
   (s/optional-key :workflow/nodes) [WorkflowNode]
   (s/optional-key :workflow/schedules) [s/Int]
   (s/optional-key :workflow/tags) [s/Int]})

(s/defschema WorkflowInfo
  (-> Workflow
      (dissoc (s/optional-key :workflow/alerts) (s/optional-key :workflow/nodes) (s/optional-key :workflow/schedules) (s/optional-key :workflow/tags))
      (assoc :workflow/alerts [Alert] :workflow/nodes [WorkflowNodeInfo] :workflow/schedules [Schedule]  :workflow/tags [Tag])))

(s/defschema WorkflowSchedules
  {:workflow/id s/Int
   :schedule/ids [s/Int]})

(s/defschema WorkflowAlerts
  {:workflow/id s/Int
   :alert/ids [s/Int]})

(s/defschema WorkflowTags
  {:workflow/id s/Int
   :tag/ids [s/Int]})

(defn new-workflow
  []
  {:workflow/name (u/uuid)
   :workflow/desc ""
   :workflow/enabled? true})


(s/defschema Variable
  {:id s/Int
   :node-id s/Int
   :name s/Str
   :value s/Str
   :is-env s/Bool})

(s/defschema GoogleAuthSettings
  {(s/optional-key :db/id) s/Int
   :setting.auth.google/client-id s/Str
   :setting.auth.google/allowed-domain s/Str})

(s/defschema SmtpSettings
  {(s/optional-key :db/id) s/Int
   :setting.smtp/host s/Str
   :setting.smtp/port s/Int
   :setting.smtp/security s/Keyword
   :setting.smtp/username s/Str
   :setting.smtp/password s/Str})

(s/defschema SlackSettings
  {(s/optional-key :db/id) s/Int
   :setting.slack/token s/Str})

(s/defschema SiteSettings
  {(s/optional-key :db/id) s/Int
   :setting.site/name s/Str
   :setting.site/url s/Str
   :setting.site/admin-email s/Str})

(s/defschema SuggestParams
  {:token s/Str
   (s/optional-key :max_matches) s/Int
   s/Keyword s/Any})


(s/defschema ExecutableInfo
  {:exe/id s/Int
   :exe/type s/Keyword
   :exe/name s/Str})


(s/defschema ExplorerNode
  {:node/id s/Int
   :node/name s/Str
   :node/type s/Keyword})
