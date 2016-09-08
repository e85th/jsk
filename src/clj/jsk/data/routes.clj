(ns jsk.data.routes
  (:require [ring.util.http-response :as http-response]
            [jsk.data.models :as m]
            [jsk.data.schedule :as schedule]
            [jsk.data.agent :as agent]
            [jsk.data.alert :as alert]
            [jsk.data.directory :as directory]
            [jsk.data.job :as job]
            [jsk.data.workflow :as workflow]
            [compojure.api.sweet :refer [defroutes context POST GET PUT DELETE]]
            [e85th.backend.web]
            [schema.core :as s]
            [taoensso.timbre :as log]))

(defroutes agent-routes
  (context "/v1/agent" [] :tags ["agent"]
    :components [res]
    (GET "/:id" []
      :return m/Agent
      :path-params [id :- s/Int]
      :exists [found (agent/find-by-id res id)]
      (http-response/ok found))

    (POST "/" []
      :return m/Agent
      :body [agent-info m/AgentCreate]
      :auth [user]
      (http-response/created nil (agent/create res agent-info (:id user))))

    (PUT "/:id" []
      :return m/Agent
      :path-params [id :- s/Int]
      :body [agent-info m/AgentAmend]
      :exists [_ (agent/find-by-id res id)]
      :auth [user]
      (http-response/ok (agent/amend res id agent-info (:id user))))

    (DELETE "/:id" []
      :return m/Agent
      :path-params [id :- s/Int]
      :exists [found (agent/find-by-id res id)]
      :auth [user]
      (agent/delete res id (:id user))
      (http-response/ok found))))

(defroutes alert-routes
  (context "/v1/alert" [] :tags ["alert"]
    :components [res]
    (GET "/:id" []
      :return m/Alert
      :path-params [id :- s/Int]
      :exists [found (alert/find-by-id res id)]
      (http-response/ok found))

    (POST "/" []
      :return m/Alert
      :body [alert-info m/AlertCreate]
      :auth [user]
      (http-response/created nil (alert/create res alert-info (:id user))))

    (PUT "/:id" []
      :return m/Alert
      :path-params [id :- s/Int]
      :body [alert-info m/AlertAmend]
      :exists [_ (alert/find-by-id res id)]
      :auth [user]
      (http-response/ok (alert/amend res id alert-info (:id user))))

    (DELETE "/:id" []
      :return m/Alert
      :path-params [id :- s/Int]
      :exists [found (alert/find-by-id res id)]
      :auth [user]
      (alert/delete res id (:id user))
      (http-response/ok found))))


(defroutes schedule-routes
  (context "/v1/schedule" [] :tags ["schedule"]
    :components [res]
    (GET "/:id" []
      :return m/Schedule
      :path-params [id :- s/Int]
      :exists [found (schedule/find-by-id res id)]
      (http-response/ok found))

    (POST "/" []
      :return m/Schedule
      :body [schedule-info m/ScheduleCreate]
      :auth [user]
      (http-response/created nil (schedule/create res schedule-info (:id user))))

    (PUT "/:id" []
      :return m/Schedule
      :path-params [id :- s/Int]
      :body [schedule-info m/ScheduleAmend]
      :exists [_ (schedule/find-by-id res id)]
      :auth [user]
      (http-response/ok (schedule/amend res id schedule-info (:id user))))

    (DELETE "/:id" []
      :return m/Schedule
      :path-params [id :- s/Int]
      :exists [found (schedule/find-by-id res id)]
      :auth [user]
      (schedule/delete res id (:id user))
      (http-response/ok found))

    (POST "/actions/create-job-assoc" []
      :return m/JobScheduleInfo
      :body [sched-assoc m/ScheduleJobAssoc]
      :auth [user]
      (http-response/ok (schedule/create-job-assoc res sched-assoc (:id user))))

    (POST "/actions/remove-job-assoc" []
      :return m/JobScheduleInfo
      :body [{:keys [id]} {:id s/Int}]
      :auth [user]
      :exists [found (schedule/find-job-schedule-by-id res id)]
      (schedule/remove-job-assoc res id (:id user))
      (http-response/ok found))

    (POST "/actions/create-workflow-assoc" []
      :return m/WorkflowScheduleInfo
      :body [sched-assoc m/ScheduleWorkflowAssoc]
      :auth [user]
      (http-response/ok (schedule/create-workflow-assoc res sched-assoc (:id user))))

    (POST "/actions/remove-workflow-assoc" []
      :return m/WorkflowScheduleInfo
      :body [{:keys [id]} {:id s/Int}]
      :auth [user]
      :exists [found (schedule/find-workflow-schedule-by-id res id)]
      (schedule/remove-workflow-assoc res id (:id user))
      (http-response/ok found))))

(defroutes directory-routes
  (context "/v1/directory" [] :tags ["directory"]
    :components [res]
    (GET "/:id" []
      :return m/Directory
      :path-params [id :- s/Int]
      :exists [found (directory/find-by-id res id)]
      (http-response/ok found))

    (POST "/" []
      :return m/Directory
      :body [directory-info m/DirectoryCreate]
      :auth [user]
      (http-response/created nil (directory/create res directory-info (:id user))))

    (PUT "/:id" []
      :return m/Directory
      :path-params [id :- s/Int]
      :body [directory-info m/DirectoryAmend]
      :exists [_ (directory/find-by-id res id)]
      :auth [user]
      (http-response/ok (directory/amend res id directory-info (:id user))))

    (DELETE "/:id" []
      :return m/Directory
      :path-params [id :- s/Int]
      :exists [found (directory/find-by-id res id)]
      :auth [user]
      (directory/delete res id (:id user))
      (http-response/ok found))))

(defroutes job-routes
  (context "/v1/job" [] :tags ["job"]
    :components [res]
    (GET "/:id" []
      :return m/Job
      :path-params [id :- s/Int]
      :exists [found (job/find-by-id res id)]
      (http-response/ok found))

    (POST "/" []
      :return m/Job
      :body [job-info m/JobCreate]
      :auth [user]
      (http-response/created nil (job/create res job-info (:id user))))

    (PUT "/:id" []
      :return m/Job
      :path-params [id :- s/Int]
      :body [job-info m/JobAmend]
      :exists [_ (job/find-by-id res id)]
      :auth [user]
      (http-response/ok (job/amend res id job-info (:id user))))

    (DELETE "/:id" []
      :return m/Job
      :path-params [id :- s/Int]
      :exists [found (job/find-by-id res id)]
      :auth [user]
      (job/delete res id (:id user))
      (http-response/ok found))))

(defroutes workflow-routes
  (context "/v1/workflow" [] :tags ["workflow"]
    :components [res]
    (GET "/:id" []
      :return m/Workflow
      :path-params [id :- s/Int]
      :exists [found (workflow/find-by-id res id)]
      (http-response/ok found))

    (POST "/" []
      :return m/Workflow
      :body [workflow-info m/WorkflowCreate]
      :auth [user]
      (http-response/created nil (workflow/create res workflow-info (:id user))))

    (PUT "/:id" []
      :return m/Workflow
      :path-params [id :- s/Int]
      :body [workflow-info m/WorkflowAmend]
      :exists [_ (workflow/find-by-id res id)]
      :auth [user]
      (http-response/ok (workflow/amend res id workflow-info (:id user))))

    (DELETE "/:id" []
      :return m/Workflow
      :path-params [id :- s/Int]
      :exists [found (workflow/find-by-id res id)]
      :auth [user]
      (workflow/delete res id (:id user))
      (http-response/ok found))))
