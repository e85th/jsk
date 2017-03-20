(ns jsk.data.routes
  (:require [ring.util.http-response :as http-response]
            [jsk.data.models :as m]
            [jsk.data.schedule :as schedule]
            [jsk.data.agent :as agent]
            [jsk.data.alert :as alert]
            [jsk.data.tag :as tag]
            [jsk.data.job :as job]
            [jsk.data.job-types :as job-types]
            [jsk.data.workflow :as workflow]
            [jsk.data.user :as user]
            [jsk.data.search :as search]
            [jsk.data.explorer :as explorer]
            [compojure.api.sweet :refer [defroutes context POST GET PUT DELETE]]
            [e85th.backend.web]
            [e85th.backend.core.models :as cm]
            [schema.core :as s]
            [taoensso.timbre :as log]))

(defroutes tag-routes
  (context "/v1/tags" [] :tags ["tag"]
    :components [res]
    (GET "/" []
      :return [m/Tag]
      :auth [user]
      (http-response/ok (tag/find-all res)))

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
      (->> (tag/create res tag (:db/id user))
           (tag/find-by-id! res)
           (http-response/created nil)))

    (PUT "/:id" []
      :return m/Tag
      :path-params [id :- s/Int]
      :body [tag m/Tag]
      :exists [_ (tag/find-by-id res id)]
      :auth [user]
      (tag/modify res id tag (:db/id user))
      (http-response/ok (tag/find-by-id! res id)))

    (DELETE "/:id" []
      :return m/Tag
       :path-params [id :- s/Int]
       :exists [found (tag/find-by-id res id)]
       :auth [user]
       (tag/rm res id (:db/id user))
       (http-response/ok found))))

(defroutes agent-routes
  (context "/v1/agents" [] :tags ["agent"]
    :components [res]
    (GET "/" []
      :return [m/Agent]
      :auth [user]
      (http-response/ok (agent/find-all res)))

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
      (->> (agent/create res agent (:db/id user))
           (agent/find-by-id! res)
           (http-response/created nil)))

    (PUT "/:id" []
      :return m/Agent
      :path-params [id :- s/Int]
      :body [agent m/Agent]
      :exists [_ (agent/find-by-id res id)]
      :auth [user]
      (agent/modify res id agent (:db/id user))
      (http-response/ok (agent/find-by-id! res id)))

    (DELETE "/:id" []
      :return m/Agent
      :path-params [id :- s/Int]
      :exists [found (agent/find-by-id res id)]
      :auth [user]
      (agent/rm res id 1 #_(:db/id user))
      (http-response/ok found))))

(defroutes alert-routes
  (context "/v1/alerts" [] :tags ["alert"]
    :components [res]

    (GET "/" []
      :return [m/Alert]
      :auth [user]
      (http-response/ok (alert/find-all res)))

    (GET "/:id" []
      :return m/AlertInfo
      :path-params [id :- s/Int]
      :exists [found (alert/find-info-by-id! res id)]
      :auth [user]
      (http-response/ok found))

    (POST "/" []
      :return m/AlertInfo
      :body [alert m/Alert]
      :auth [user]
      (->> (alert/create res alert (:db/id user))
           (alert/find-info-by-id! res)
           (http-response/created nil)))

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
      :return m/AlertInfo
      :path-params [id :- s/Int]
      :body [alert m/Alert]
      :exists [_ (alert/find-by-id res id)]
      :auth [user]
      (alert/modify res id alert (:db/id user))
      (http-response/ok (alert/find-info-by-id! res id)))

    (DELETE "/:id" []
      :return m/AlertInfo
      :path-params [id :- s/Int]
      :exists [found (alert/find-info-by-id! res id)]
      :auth [user]
      (alert/rm res id (:db/id user))
      (http-response/ok found))))


(defroutes schedule-routes
  (context "/v1/schedules" [] :tags ["schedule"]
    :components [res]

    (GET "/" []
      :return [m/Schedule]
      :auth [user]
      (http-response/ok (schedule/find-all res)))

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
      (->> (schedule/create res schedule (:db/id user))
           (schedule/find-by-id! res)
           (http-response/created nil)))

    (PUT "/:id" []
      :return m/Schedule
      :path-params [id :- s/Int]
      :body [schedule m/Schedule]
      :exists [_ (schedule/find-by-id res id)]
      :auth [user]
      (schedule/modify res id schedule (:db/id user))
      (http-response/ok (schedule/find-by-id! res id)))

    (DELETE "/:id" []
      :return m/Schedule
      :path-params [id :- s/Int]
      :exists [found (schedule/find-by-id res id)]
      :auth [user]
      (schedule/rm res id (:db/id user))
      (http-response/ok found))))

(defroutes job-routes
  (context "/v1/jobs" [] :tags ["job"]
    :components [res]

    (GET "/" []
      :return [m/Job]
      :auth [user]
      (http-response/ok (job/find-all res)))

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
      (->> (job/create res job (:db/id user))
           (job/find-by-id! res)
           (http-response/created nil )))

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
      (job/modify res id job (:db/id user))
      (http-response/ok (job/find-by-id! res id)))

    (DELETE "/:id" []
      :return m/Job
      :path-params [id :- s/Int]
      :auth [user]
      :exists [found (job/find-by-id res id)]
      (job/rm res id (:db/id user))
      (http-response/ok found))))

(defroutes job-type-routes
  (context "/v1/job-types" [] :tags ["job-types"]
    :components [res]
    (GET "/" []
      :return m/JobTypeSchema
      :auth [user]
      (http-response/ok (job-types/find-all res)))))

(defroutes workflow-routes
  (context "/v1/workflows" [] :tags ["workflow"]
    :components [res]

    (GET "/" []
      :return [m/Workflow]
      :auth [user]
      (http-response/ok (workflow/find-all res)))

    (GET "/:id" []
      :return m/Workflow
      :path-params [id :- s/Int]
      :exists [found (workflow/find-by-id res id)]
      (http-response/ok found))

    (POST "/" []
      :return m/Workflow
      :body [wf m/Workflow]
      :auth [user]
      (->> (workflow/create res wf (:db/id user))
           (workflow/find-by-id! res)
           (http-response/created nil)))

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
      (workflow/modify res id wf (:db/id user))
      (http-response/ok (workflow/find-by-id! res id)))

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
    (GET "/" []
      :summary "Return information about the current user."
      :return [m/User]
      :auth [user]
      (http-response/ok (user/find-user-by-id! res (:db/id user))))

    (POST "/actions/authenticate" []
      :summary "Authentication based on firebase, nonce or password."
      :return m/AuthResponse
      :body [user-auth cm/UserAuthRequest]
      (http-response/ok (user/authenticate res user-auth)))))

(defroutes search-routes
  (context "/v1/search" [] :tags ["search"]
    :components [res]
    (GET "/suggest" {:keys [params]}
      :summary "Returns channel suggestions"
      ;:query-params [q :- s/Str]
      :auth [user]
      (http-response/ok (search/suggest-channels res (or (:q params)
                                                         (:token params)
                                                         ""))))))


(defroutes explorer-routes
  (context "/v1/explorer" [] :tags ["explorer"]
    :components [res]
    (GET "/" []
      :summary "Returns data for the explorer view."
      :return [m/ExplorerNode]
      :query-params [type :- s/Keyword]
      :auth [user]
      (http-response/ok (explorer/get-nodes res type)))))
