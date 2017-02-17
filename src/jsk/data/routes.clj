(ns jsk.data.routes
  (:require [ring.util.http-response :as http-response]
            [jsk.data.models :as m]
            [jsk.data.schedule :as schedule]
            [jsk.data.agent :as agent]
            [jsk.data.alert :as alert]
            [jsk.data.tag :as tag]
            [jsk.data.job :as job]
            [jsk.data.workflow :as workflow]
            [jsk.data.user :as user]
            [compojure.api.sweet :refer [defroutes context POST GET PUT DELETE]]
            [e85th.backend.web]
            [e85th.backend.core.models :as cm]
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
      :auth [user]
      :exists [found (agent/find-by-id res id)]
      (http-response/ok found))

    (POST "/" []
      :return m/Agent
      :body [agent m/Agent]
      :auth [user]
      (http-response/created nil (agent/create res agent 1 #_(:db/id user))))

    (PUT "/:id" []
      :return m/Agent
      :path-params [id :- s/Int]
      :body [agent m/Agent]
      :exists [_ (agent/find-by-id res id)]
      :auth [user]
      (http-response/ok (agent/modify res id agent 1 #_(:db/id user))))

    (DELETE "/:id" []
      :return m/Agent
      :path-params [id :- s/Int]
      :exists [found (agent/find-by-id res id)]
      :auth [user]
      (agent/rm res id 1 #_(:db/id user))
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

    (POST "/actions/assoc-channels" []
          :summary "Associates channels to a alert. Returns the new set of associated channels ."
          :return m/AlertChannels
          :body [data m/AlertChannels]
          :auth [user]
          (http-response/ok
           (alert/assoc-channels res (:alert/id data) (:channel/ids data) (:db/id user))))

    (POST "/actions/dissoc-channels" []
          :summary "Dissociates channels from a alert. Returns the new set of associated channels."
          :return m/AlertChannels
          :body [data m/AlertChannels]
          :auth [user]
          (http-response/ok
           (alert/dissoc-channels res (:alert/id data) (:channel/ids data) (:db/id user))))

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

    (POST "/actions/assoc-schedules" []
      :summary "Associates schedules to a job. Returns the new set of associated schedules ."
      :return m/JobSchedules
      :body [data m/JobSchedules]
      :auth [user]
      (http-response/ok
       (job/assoc-schedules res (:job/id data) (:schedule/ids data) (:db/id user))))

    (POST "/actions/dissoc-schedules" []
      :summary "Dissociates schedules from a job. Returns the new set of associated schedules."
      :return m/JobSchedules
      :body [data m/JobSchedules]
      :auth [user]
      (http-response/ok
       (job/dissoc-schedules res (:job/id data) (:schedule/ids data) (:db/id user))))

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
      :body [wf m/Workflow]
      :auth [user]
      (http-response/created nil (workflow/create res wf (:db/id user))))

    (POST "/actions/assoc-schedules" []
      :summary "Associates schedules to a workflow. Returns the new set of associated schedules ."
      :return m/WorkflowSchedules
      :body [data m/WorkflowSchedules]
      :auth [user]
      (http-response/ok
       (workflow/assoc-schedules res (:workflow/id data) (:schedule/ids data) (:db/id user))))

    (POST "/actions/dissoc-schedules" []
      :summary "Dissociates schedules from a workflow. Returns the new set of associated schedules."
      :return m/WorkflowSchedules
      :body [data m/WorkflowSchedules]
      :auth [user]
      (http-response/ok
       (workflow/dissoc-schedules res (:workflow/id data) (:schedule/ids data) (:db/id user))))

    (PUT "/:id" []
      :return m/Workflow
      :path-params [id :- s/Int]
      :body [wf m/Workflow]
      :auth [user]
      :exists [_ (workflow/find-by-id res id)]
      (http-response/ok (workflow/modify res id wf (:db/id user))))

    (DELETE "/:id" []
      :return m/Workflow
      :path-params [id :- s/Int]
      :exists [found (workflow/find-by-id res id)]
      :auth [user]
      (workflow/rm res id (:db/id user))
      (http-response/ok found))))


(defroutes user-routes
  (context "/v1/users" [] :tags ["users"]
    :components [res]
    (POST "/actions/authenticate" []
      :summary "Authentication based on firebase, nonce or password."
      :return m/AuthResponse
      :body [user-auth cm/UserAuthRequest]
      (http-response/ok (user/authenticate res user-auth)))))
