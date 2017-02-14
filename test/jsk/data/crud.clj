(ns jsk.data.crud
  (:require [clojure.test :refer :all]
            [jsk.core-test :as test]
            [taoensso.timbre :as log]
            [e85th.commons.util :as u]
            [jsk.data.models :as m]
            [e85th.commons.ex :as ex]))

(def schedule-base-endpoint "/api/v1/schedule")

(defn create-schedule
  "Create and return the schedule data."
  ([expected-status data]
   (let [[status response] (test/api-call :post schedule-base-endpoint data)]
     (is (= expected-status status))
     response))
  ([]
   (let [schedule-name (u/uuid)
         data {:schedule/name schedule-name
               :schedule/desc "Schedule description"
               :schedule/cron-expr "0 0 12 ? * WED"}
         schedule (create-schedule 201 data)]
     (is (= schedule-name (:schedule/name schedule)))
     schedule)))

(defn update-schedule
  ([expected-status data schedule-id]
   (let [endpoint (str schedule-base-endpoint "/" schedule-id)
         [status response] (test/api-call :put endpoint data)]
     (is (= expected-status status))
     response))
  ([data schedule-id]
   (update-schedule 200 data schedule-id)))

(defn get-schedule
  ([expected-status schedule-id]
   (let [endpoint (str schedule-base-endpoint "/" schedule-id)
         [status response] (test/api-call :get endpoint)]
     (is (= expected-status status))
     response))
  ([schedule-id]
   (let [schedule (get-schedule 200 schedule-id)]
     (is (= schedule-id (:db/id schedule))))))

(defn delete-schedule
  ([expected-status schedule-id]
   (let [endpoint (str schedule-base-endpoint "/" schedule-id)
         [status response] (test/api-call :delete endpoint)]
     (is (= expected-status status))
     response))
  ([schedule-id]
   (let [schedule (delete-schedule 200 schedule-id)]
     (is (= schedule-id (:db/id schedule))))))


;; -- job schedule

(defn create-schedule-job-assoc
  [schedule-id job-id]
  (let [endpoint (str schedule-base-endpoint "/actions/create-job-assoc")
        data {:schedule-id schedule-id :job-id job-id}
        [status sched-job] (test/api-call :post endpoint data)]
    (is (= 200 status))
    (is (= data (select-keys sched-job [:schedule-id :job-id])))
    sched-job))

(defn delete-schedule-job-assoc
  [id]
  (let [endpoint (str schedule-base-endpoint "/actions/remove-job-assoc")
        data {:id id}
        [status sched-job] (test/api-call :post endpoint data)]
    (is (= 200 status))
    (is (= id (:id sched-job)))))


;; -- workflow schedule

(defn create-schedule-workflow-assoc
  [schedule-id workflow-id]
  (let [endpoint (str schedule-base-endpoint "/actions/create-workflow-assoc")
        data {:schedule-id schedule-id :workflow-id workflow-id}
        [status sched-wf] (test/api-call :post endpoint data)]
    (is (= 200 status))
    (is (= data (select-keys sched-wf [:schedule-id :workflow-id])))
    sched-wf))

(defn delete-schedule-workflow-assoc
  [id]
  (let [endpoint (str schedule-base-endpoint "/actions/remove-workflow-assoc")
        data {:id id}
        [status sched-wf] (test/api-call :post endpoint data)]
    (is (= 200 status))
    (is (= id (:id sched-wf)))))

;; -- workflow
(def workflow-base-endpoint "/api/v1/workflow")

(defn create-workflow
  "Creates a workflow."
  ([expected-status data]
   (let [[status response] (test/api-call :post workflow-base-endpoint data)]
     (is (= expected-status status))
     response))
  ([]
   (let [data {:name (u/uuid)
               :description "workflow description"
               :is-enabled true
               :tags ["shell-workflow"]}
         workflow (create-workflow 201 data)]
     (is (= data (select-keys workflow (keys data))))
     workflow)))

(defn update-workflow
  ([expected-status data workflow-id]
   (let [endpoint (str workflow-base-endpoint "/" workflow-id)
         [status response] (test/api-call :put endpoint data)]
     (is (= expected-status status))
     response))
  ([data workflow-id]
   (update-workflow 200 data workflow-id)))

(defn get-workflow
  ([expected-status workflow-id]
   (let [endpoint (str workflow-base-endpoint "/" workflow-id)
         [status response] (test/api-call :get endpoint)]
     (is (= expected-status status))
     response))
  ([workflow-id]
   (let [workflow (get-workflow 200 workflow-id)]
     (is (= workflow-id (:id workflow))))))

(defn delete-workflow
  ([expected-status workflow-id]
   (let [endpoint (str workflow-base-endpoint "/" workflow-id)
         [status response] (test/api-call :delete endpoint)]
     (is (= expected-status status))
     response))
  ([workflow-id]
   (let [workflow (delete-workflow 200 workflow-id)]
     (is (= workflow-id (:id workflow))))))

;; -- job
(def job-base-endpoint "/api/v1/job")

(defn create-job
  "Creates a job."
  ([expected-status data]
   (let [[status response] (test/api-call :post job-base-endpoint data)]
     (is (= expected-status status))
     response))
  ([]
   (let [data {:job/name (u/uuid)
               :job/desc "job description"
               :job/enabled? true
               :job/type :shell
               :job/props {:working-dir "/home/jsk" :command "ls -lht"}
               :job/behavior :standard
               :job/max-retries 0
               :job/max-concurrent 1
               :job/timeout-ms 0}
         job (create-job 201 data)]
     (is (= data (select-keys job (keys data))))
     job)))

(defn update-job
  ([expected-status data job-id]
   (let [endpoint (str job-base-endpoint "/" job-id)
         [status response] (test/api-call :put endpoint data)]
     (is (= expected-status status))
     response))
  ([data job-id]
   (update-job 200 data job-id)))

(defn get-job
  ([expected-status job-id]
   (let [endpoint (str job-base-endpoint "/" job-id)
         [status response] (test/api-call :get endpoint)]
     (is (= expected-status status))
     response))
  ([job-id]
   (let [job (get-job 200 job-id)]
     (is (= job-id (:db/id job))))))

(defn delete-job
  ([expected-status job-id]
   (let [endpoint (str job-base-endpoint "/" job-id)
         [status response] (test/api-call :delete endpoint)]
     (is (= expected-status status))
     response))
  ([job-id]
   (let [job (delete-job 200 job-id)]
     (is (= job-id (:db/id job))))))
