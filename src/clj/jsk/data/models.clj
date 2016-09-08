(ns jsk.data.models
  (:require [schema.core :as s]
            [e85th.commons.util :as u]
            [clojure.set :as set]))

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

;; -- Agent
(s/defschema Agent
  {:id s/Int
   :name s/Str})

(s/defschema AgentCreate
  (dissoc Agent :id))

(s/defschema AgentAmend
  (u/make-all-keys-optional AgentCreate))

;; -- Alert
(s/defschema Alert
  {:id s/Int
   :name s/Str
   :description (s/maybe s/Str)})

(s/defschema AlertCreate
  (dissoc Alert :id))

(s/defschema AlertAmend
  (u/make-all-keys-optional AlertCreate))

;; -- Schedule
(s/defschema Schedule
  {:id s/Int
   :name s/Str
   :description (s/maybe s/Str)
   :cron-expr s/Str})

(s/defschema ScheduleCreate
  (dissoc Schedule :id))

(s/defschema ScheduleAmend
  (u/make-all-keys-optional Schedule))

;; -- Directory
(s/defschema Directory
  {:id s/Int
   :name s/Str
   :parent-id (s/maybe s/Int)})

(s/defschema DirectoryCreate
  (dissoc Directory :id))

(s/defschema DirectoryAmend
  (u/make-all-keys-optional DirectoryCreate))

;; items that can be in a directory, other directories, jobs, and workflows
(s/defschema DirectoryItem
  {:id s/Int
   :name s/Str
   :is-enabled s/Bool
   :type s/Keyword})

;; -- Node
(s/defschema Node
  {:name s/Str
   :description (s/maybe s/Str)
   :is-enabled s/Bool
   :tags [s/Str]})

;; -- Job
(s/defschema Job
  {:id s/Int
   :name s/Str
   :description (s/maybe s/Str)
   :is-enabled s/Bool
   :agent-id (s/maybe s/Int)
   :type s/Str
   :props {s/Keyword s/Any}
   :behavior-id s/Int
   :context-key (s/maybe s/Str)
   :max-retries s/Int
   :max-concurrent s/Int
   :timeout s/Int
   :tags [s/Str]})

(s/defschema JobCreate
  {:name s/Str
   :type s/Str
   :props {s/Keyword s/Any}
   (s/optional-key :description) s/Str
   (s/optional-key :is-enabled) s/Bool
   (s/optional-key :agent-id) s/Int
   (s/optional-key :behavior-id) s/Int
   (s/optional-key :context-key) s/Str
   (s/optional-key :max-retries) s/Int
   (s/optional-key :max-concurrent) s/Int
   (s/optional-key :timeout) s/Int
   (s/optional-key :tags) [s/Str]})

(s/defschema JobAmend
  (u/make-all-keys-optional JobCreate))

;; -- Workflow
(s/defschema Workflow
  {:id s/Int
   :name s/Str
   :description (s/maybe s/Str)
   :is-enabled s/Bool
   :tags [s/Str]})

(s/defschema WorkflowCreate
  {:name s/Str
   (s/optional-key :description) (s/maybe s/Str)
   (s/optional-key :is-enabled) s/Bool
   (s/optional-key :tags) [s/Str]})

(s/defschema WorkflowAmend
  (u/make-all-keys-optional WorkflowCreate))


(s/defschema Variable
  {:id s/Int
   :node-id s/Int
   :name s/Str
   :value s/Str
   :is-env s/Bool})

(s/defschema VariableCreate
  {:name s/Str
   :value s/Str
   :is-env s/Bool})

(s/defschema VariableAmend
  (u/make-all-keys-optional VariableCreate))


(s/defschema JobScheduleInfo
  {:id s/Int
   :job-id s/Int
   :schedule-id s/Int
   :schedule-name s/Str})


(s/defschema ScheduleJobAssoc
  {:schedule-id s/Int
   :job-id s/Int})

(s/defschema ScheduleJobDissoc
  {:schedule-id s/Int
   :job-id s/Int})

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
