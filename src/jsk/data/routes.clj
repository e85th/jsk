(ns jsk.data.routes
  (:require [ring.util.http-response :as http-response]
            [jsk.data.models :as m]
            [jsk.data.schedule :as schedule]
            [jsk.data.agent :as agent]
            [jsk.data.alert :as alert]
            [jsk.data.tag :as tag]
            [jsk.data.job :as job]
            [jsk.data.workflow :as workflow]
            [compojure.api.sweet :refer [defroutes context POST GET PUT DELETE]]
            [e85th.backend.web]
            [schema.core :as s]
            [taoensso.timbre :as log]))

(defroutes tag-routes
  (context "/v1/tag" [] :tags ["tag"]
    :components [res]
    (GET "/:id" []
      :return m/Tag
      :path-params [id :- s/Int]
      :exists [found (tag/find-by-id res id)]
      :auth [user]
      (http-response/ok found))

    (POST "/" []
      :return m/Tag
      :body [tag m/Tag]
      :auth [user]
      (http-response/created nil (tag/create res tag (:db/id user))))

    (PUT "/:id" []
      :return m/Tag
      :path-params [id :- s/Int]
      :body [tag m/Tag]
      :exists [_ (tag/find-by-id res id)]
      :auth [user]
      (http-response/ok (tag/modify res id tag (:db/id user))))

    (DELETE "/:id" []
      :return m/Tag
       :path-params [id :- s/Int]
       :exists [found (tag/find-by-id res id)]
       :auth [user]
       (tag/rm res id (:db/id user))
       (http-response/ok found))))

(defroutes agent-routes
  (context "/v1/agent" [] :tags ["agent"]
    :components [res]
    (GET "/:id" []
      :return m/Agent
      :path-params [id :- s/Int]
      :exists [found (agent/find-by-id res id)]
      :auth [user]
      (http-response/ok found))

    (POST "/" []
      :return m/Agent
      :body [agent m/Agent]
      :auth [user]
      (http-response/created nil (agent/create res agent (:db/id user))))

    (PUT "/:id" []
      :return m/Agent
      :path-params [id :- s/Int]
      :body [agent m/Agent]
      :exists [_ (agent/find-by-id res id)]
      :auth [user]
      (http-response/ok (agent/modify res id agent (:db/id user))))

    (DELETE "/:id" []
      :return m/Agent
      :path-params [id :- s/Int]
      :exists [found (agent/find-by-id res id)]
      :auth [user]
      (agent/rm res id (:db/id user))
      (http-response/ok found))))

(defroutes alert-routes
  (context "/v1/alert" [] :tags ["alert"]
    :components [res]
    (GET "/:id" []
      :return m/Alert
      :path-params [id :- s/Int]
      :exists [found (alert/find-by-id res id)]
      :auth [user]
      (http-response/ok found))

    (POST "/" []
      :return m/Alert
      :body [alert m/Alert]
      :auth [user]
      (http-response/created nil (alert/create res alert (:db/id user))))

    (PUT "/:id" []
      :return m/Alert
      :path-params [id :- s/Int]
      :body [alert m/Alert]
      :exists [_ (alert/find-by-id res id)]
      :auth [user]
      (http-response/ok (alert/modify res id alert (:db/id user))))

    (DELETE "/:id" []
      :return m/Alert
      :path-params [id :- s/Int]
      :exists [found (alert/find-by-id res id)]
      :auth [user]
      (alert/rm res id (:db/id user))
      (http-response/ok found))))


(defroutes schedule-routes
  (context "/v1/schedule" [] :tags ["schedule"]
    :components [res]
    (GET "/:id" []
      :return m/Schedule
      :path-params [id :- s/Int]
      :exists [found (schedule/find-by-id res id)]
      :auth [user]
      (http-response/ok found))

    (POST "/" []
      :return m/Schedule
      :body [schedule m/Schedule]
      :auth [user]
      (http-response/created nil (schedule/create res schedule (:db/id user))))

    (PUT "/:id" []
      :return m/Schedule
      :path-params [id :- s/Int]
      :body [schedule m/Schedule]
      :exists [_ (schedule/find-by-id res id)]
      :auth [user]
      (http-response/ok (schedule/modify res id schedule (:db/id user))))

    (DELETE "/:id" []
      :return m/Schedule
      :path-params [id :- s/Int]
      :exists [found (schedule/find-by-id res id)]
      :auth [user]
      (schedule/rm res id (:db/id user))
      (http-response/ok found))))

;;     (POST "/actions/create-job-assoc" []
;;       :return m/JobScheduleInfo
;;       :body [sched-assoc m/ScheduleJobAssoc]
;;       :auth [user]
;;       (http-response/ok (schedule/create-job-assoc res sched-assoc (:id user))))

;;     (POST "/actions/remove-job-assoc" []
;;       :return m/JobScheduleInfo
;;       :body [{:keys [id]} {:id s/Int}]
;;       :auth [user]
;;       :exists [found (schedule/find-job-schedule-by-id res id)]
;;       (schedule/remove-job-assoc res id (:id user))
;;       (http-response/ok found))

;;     (POST "/actions/create-workflow-assoc" []
;;       :return m/WorkflowScheduleInfo
;;       :body [sched-assoc m/ScheduleWorkflowAssoc]
;;       :auth [user]
;;       (http-response/ok (schedule/create-workflow-assoc res sched-assoc (:id user))))

;;     (POST "/actions/remove-workflow-assoc" []
;;       :return m/WorkflowScheduleInfo
;;       :body [{:keys [id]} {:id s/Int}]
;;       :auth [user]
;;       :exists [found (schedule/find-workflow-schedule-by-id res id)]
;;       (schedule/remove-workflow-assoc res id (:id user))
;;       (http-response/ok found))))

(defroutes job-routes
  (context "/v1/job" [] :tags ["job"]
    :components [res]
    (GET "/:id" []
      :return m/Job
      :path-params [id :- s/Int]
      :auth [user]
      :exists [found (job/find-by-id res id)]
      (http-response/ok found))

    (POST "/" []
      :return m/Job
      :body [job m/Job]
      :auth [user]
      (http-response/created nil (job/create res job (:db/id user))))

    (PUT "/:id" []
      :return m/Job
      :path-params [id :- s/Int]
      :body [job m/Job]
      :auth [user]
      :exists [_ (job/find-by-id res id)]
      (http-response/ok (job/modify res id job (:db/id user))))

    (DELETE "/:id" []
      :return m/Job
      :path-params [id :- s/Int]
      :auth [user]
      :exists [found (job/find-by-id res id)]
      (job/rm res id (:db/id user))
      (http-response/ok found))))

;; (defroutes workflow-routes
;;   (context "/v1/workflow" [] :tags ["workflow"]
;;     :components [res]
;;     (GET "/:id" []
;;       :return m/Workflow
;;       :path-params [id :- s/Int]
;;       :exists [found (workflow/find-by-id res id)]
;;       (http-response/ok found))

;;     (POST "/" []
;;       :return m/Workflow
;;       :body [workflow-info m/WorkflowCreate]
;;       :auth [user]
;;       (http-response/created nil (workflow/create res workflow-info (:id user))))

;;     (PUT "/:id" []
;;       :return m/Workflow
;;       :path-params [id :- s/Int]
;;       :body [workflow-info m/WorkflowAmend]
;;       :exists [_ (workflow/find-by-id res id)]
;;       :auth [user]
;;       (http-response/ok (workflow/amend res id workflow-info (:id user))))

;;     (DELETE "/:id" []
;;       :return m/Workflow
;;       :path-params [id :- s/Int]
;;       :exists [found (workflow/find-by-id res id)]
;;       :auth [user]
;;       (workflow/delete res id (:id user))
;;       (http-response/ok found))))
