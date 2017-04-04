(ns jsk.net.api
  (:require [e85th.ui.net.rpc :as rpc]
            [taoensso.timbre :as log]
            [jsk.common.data :as data]
            [re-frame.core :as rf]
            [schema.core :as s]
            [e85th.ui.util :as u]))

(s/defn ^:private full-url
  [url-path]
  (str (data/api-host) "/api" url-path))

(defn suggest-url
  []
  (full-url "/v1/search/suggest"))

(s/defn new-request
  ([method url ok err]
   (new-request method url {} ok err))
  ([method url params ok err]
   (-> (rpc/new-re-frame-request method (full-url url) params ok err)
       rpc/with-edn-format
       (rpc/with-bearer-auth (data/jsk-token)))))

(s/defn call!
  "For testing really. Use effects to actually make calls.
   re-frame handling. ok and err can be either keywords or a vector.
   If vector then the first should be a keyword to conform to re-frame dispatch
   semantics."
  ([method url params ok err]
   (call! (new-request method url params ok err)))
  ([req]
   (let [ensure-handler-fn (fn [{:keys [handler error-handler] :as r}]
                             (cond-> r
                               (vector? handler) (assoc :handler #(rf/dispatch (conj handler %)))
                               (vector? error-handler) (assoc :error-handler #(rf/dispatch (conj error-handler %)))))
         normalize (comp ensure-handler-fn)]
     (rpc/call (normalize req)))))

(defn- authenticate
  [body ok err]
  (new-request :post "/v1/users/actions/authenticate" body ok err))

(s/defn authenticate-with-google
  "Generates a request map that can be executed by call!"
  [token :- s/Str ok err]
  (authenticate {:with-google {:token token}} ok err))


(s/defn authenticate-with-password
  [email :- s/Str pass :- s/Str ok err]
  (authenticate {:with-password {:email email
                                 :password pass}}
                ok err))

(s/defn save*
  "Does either a post or put."
  [base-url id-kw data ok err]
  (let [entity-id (id-kw data)
        [method url] (if entity-id
                       [:put (str base-url "/" entity-id)]
                       [:post base-url])]
    (new-request method url data ok err)))

;; -- Agents
(s/defn fetch-agent-list
  [ok err]
  (new-request :get "/v1/agents" ok err))

(s/defn save-agent
  [agent ok err]
  (save* "/v1/agents" :db/id agent ok err))

(s/defn rm-agent
  [id ok err]
  (new-request :delete (str "/v1/agents/" id) ok err))

(s/defn fetch-agent
  [agent-id ok err]
  (new-request :get  (str "/v1/agents/" agent-id) ok err))

;; -- Alerts
(s/defn fetch-alert-list
  [ok err]
  (new-request :get "/v1/alerts" ok err))

(s/defn save-alert
  [alert ok err]
  (save* "/v1/alerts" :db/id alert ok err))

(s/defn rm-alert
  [id ok err]
  (new-request :delete (str "/v1/alerts/" id) ok err))

(s/defn fetch-alert
  [alert-id ok err]
  (new-request :get  (str "/v1/alerts/" alert-id) ok err))


;; -- Schedules
(s/defn fetch-schedule-list
  [ok err]
  (new-request :get "/v1/schedules" ok err))

(s/defn save-schedule
  [schedule ok err]
  (save* "/v1/schedules" :db/id schedule ok err))

(s/defn rm-schedule
  [id ok err]
  (new-request :delete (str "/v1/schedules/" id) ok err))

(s/defn fetch-schedule
  [schedule-id ok err]
  (new-request :get  (str "/v1/schedules/" schedule-id) ok err))


;; -- Workflows
(s/defn fetch-workflow-list
  [ok err]
  (new-request :get "/v1/workflows" ok err))

(s/defn save-workflow
  [workflow ok err]
  (save* "/v1/workflows" :db/id workflow ok err))

(s/defn rm-workflow
  [id ok err]
  (new-request :delete (str "/v1/workflows/" id) ok err))

(s/defn fetch-workflow
  [workflow-id ok err]
  (new-request :get  (str "/v1/workflows/" workflow-id) ok err))

;; -- Jobs
(s/defn fetch-job-list
  [ok err]
  (new-request :get "/v1/jobs" ok err))

(s/defn save-job
  [job ok err]
  (save* "/v1/jobs" :db/id job ok err))

(s/defn rm-job
  [id ok err]
  (new-request :delete (str "/v1/jobs/" id) ok err))

(s/defn fetch-job
  [job-id ok err]
  (new-request :get  (str "/v1/jobs/" job-id) ok err))


(s/defn fetch-job-types
  [ok err]
  (new-request :get "/v1/job-types" ok err))

(s/defn suggest-channels
  [text ok err]
  (new-request :get "/v1/search/suggest" {:q text} ok err))

(s/defn assoc-alert-channels
  [alert-id :- s/Int channel-ids :- [s/Int] ok err]
  (new-request :post "/v1/alerts/actions/assoc-channels"
               {:alert/id alert-id :channel/ids channel-ids}
               ok err))

(s/defn dissoc-alert-channels
  [alert-id :- s/Int channel-ids :- [s/Int] ok err]
  (new-request :post "/v1/alerts/actions/dissoc-channels"
               {:alert/id alert-id :channel/ids channel-ids}
               ok err))

(s/defn fetch-explorer-nodes
  [type :- s/Str ok err]
  (new-request :get "/v1/explorer" {:type type} ok err))

(s/defn create-explorer-node
  [type :- s/Str ok err]
  (new-request :post "/v1/explorer/actions/create-node" {:type type} ok err))

(s/defn rm-explorer-node
  [type :- s/Str id ok err]
  (new-request :post "/v1/explorer/actions/rm-node" {:type type
                                                     :id id} ok err))


(s/defn assoc-job-schedules
  [job-id :- s/Int schedule-ids :- [s/Int] ok err]
  (new-request :post "/v1/jobs/actions/assoc-schedules"
               {:job/id job-id :schedule/ids schedule-ids}
               ok err))

(s/defn dissoc-job-schedules
  [job-id :- s/Int schedule-ids :- [s/Int] ok err]
  (new-request :post "/v1/jobs/actions/dissoc-schedules"
               {:job/id job-id :schedule/ids schedule-ids}
               ok err))

(s/defn assoc-job-alerts
  [job-id :- s/Int alert-ids :- [s/Int] ok err]
  (new-request :post "/v1/jobs/actions/assoc-alerts"
               {:job/id job-id :alert/ids alert-ids}
               ok err))

(s/defn dissoc-job-alerts
  [job-id :- s/Int alert-ids :- [s/Int] ok err]
  (new-request :post "/v1/jobs/actions/dissoc-alerts"
               {:job/id job-id :alert/ids alert-ids}
               ok err))


(s/defn assoc-workflow-schedules
  [workflow-id :- s/Int schedule-ids :- [s/Int] ok err]
  (new-request :post "/v1/workflows/actions/assoc-schedules"
               {:workflow/id workflow-id :schedule/ids schedule-ids}
               ok err))

(s/defn dissoc-workflow-schedules
  [workflow-id :- s/Int schedule-ids :- [s/Int] ok err]
  (new-request :post "/v1/workflows/actions/dissoc-schedules"
               {:workflow/id workflow-id :schedule/ids schedule-ids}
               ok err))

(s/defn assoc-workflow-alerts
  [workflow-id :- s/Int alert-ids :- [s/Int] ok err]
  (new-request :post "/v1/workflows/actions/assoc-alerts"
               {:workflow/id workflow-id :alert/ids alert-ids}
               ok err))

(s/defn dissoc-workflow-alerts
  [workflow-id :- s/Int alert-ids :- [s/Int] ok err]
  (new-request :post "/v1/workflows/actions/dissoc-alerts"
               {:workflow/id workflow-id :alert/ids alert-ids}
               ok err))

(s/defn fetch-current-user
  [ok err]
  (new-request :get "/v1/users" ok err))
