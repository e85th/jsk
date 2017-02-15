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

(def job-node-type-id 1)
(def workflow-node-type-id 2)

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

;; -- Alert
(s/defschema Alert
  {(s/optional-key :db/id) s/Int
   :alert/name s/Str
   (s/optional-key :alert/desc) s/Str})

(s/defschema AlertChannels
  {:alert/id s/Int
   :channel/ids [s/Int]})

;; -- Schedule
(s/defschema Schedule
  {(s/optional-key :db/id) s/Int
   :schedule/name s/Str
   (s/optional-key :schedule/desc) s/Str
   :schedule/cron-expr s/Str})

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
   (s/optional-key :job/schedules) [s/Int]
   (s/optional-key :job/tags) [s/Int]})

;; -- Workflow
(s/defschema Workflow
  {(s/optional-key :db/id) s/Int
   :workflow/name s/Str
   :workflow/desc (s/maybe s/Str)
   :workflow/enabled? s/Bool
   (s/optional-key :workflow/schedules) [s/Int]
   (s/optional-key :workflow/tags) [s/Str]})

(s/defschema JobSchedules
  {:job/id s/Int
   :schedule/ids [s/Int]})

(s/defschema WorkflowSchedules
  {:workflow/id s/Int
   :schedule/ids [s/Int]})

(s/defschema Variable
  {:id s/Int
   :node-id s/Int
   :name s/Str
   :value s/Str
   :is-env s/Bool})

(s/defschema JobScheduleInfo
  {:id s/Int
   :job-id s/Int
   :schedule-id s/Int
   :schedule-name s/Str})

(s/defschema ScheduleWorkflowAssoc
  {:schedule-id s/Int
   :workflow-id s/Int})

(s/defschema ScheduleWorkflowDissoc
  {:schedule-id s/Int
   :workflow-id s/Int})

(s/defschema WorkflowScheduleInfo
  {:id s/Int
   :workflow-id s/Int
   :schedule-id s/Int
   :schedule-name s/Str})
