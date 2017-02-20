(ns jsk.data.crud
  (:require [clojure.test :refer :all]
            [jsk.core-test :as test]
            [taoensso.timbre :as log]
            [e85th.commons.util :as u]
            [jsk.data.models :as m]
            [e85th.commons.ex :as ex]))

(def schedule-base-endpoint "/api/v1/schedules")

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



;; -- workflow
(def workflow-base-endpoint "/api/v1/workflows")

(defn create-workflow
  "Creates a workflow."
  ([expected-status data]
   (let [[status response] (test/api-call :post workflow-base-endpoint data)]
     (is (= expected-status status))
     response))
  ([]
   (let [data {:workflow/name (u/uuid)
               :workflow/desc "workflow description"
               :workflow/enabled? true}
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
     (is (= workflow-id (:db/id workflow))))))

(defn delete-workflow
  ([expected-status workflow-id]
   (let [endpoint (str workflow-base-endpoint "/" workflow-id)
         [status response] (test/api-call :delete endpoint)]
     (is (= expected-status status))
     response))
  ([workflow-id]
   (let [workflow (delete-workflow 200 workflow-id)]
     (is (= workflow-id (:db/id workflow))))))

;; -- job
(def job-base-endpoint "/api/v1/jobs")

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

;; -- job schedule

(defn assoc-job-schedule
  [job-id schedule-id]
  (let [endpoint (str job-base-endpoint "/actions/assoc-schedules")
        data {:job/id job-id :schedule/ids [schedule-id]}
        [status job-sched] (test/api-call :post endpoint data)
        {schedule-ids :schedule/ids} job-sched]
    (is (= 200 status))
    (is ((set schedule-ids) schedule-id))
    job-sched))


(defn dissoc-job-schedule
  [job-id schedule-id]
  (let [endpoint (str job-base-endpoint "/actions/dissoc-schedules")
        data {:job/id job-id :schedule/ids [schedule-id]}
        [status job-sched] (test/api-call :post endpoint data)
        {schedule-ids :schedule/ids} job-sched]
    (is (= 200 status))
    (is (nil? ((set schedule-ids) schedule-id)))
    job-sched))
