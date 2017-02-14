(ns jsk.data.schedule-test
  (:require [clojure.test :refer :all]
            [jsk.core-test :as test]
            [taoensso.timbre :as log]
            [e85th.commons.util :as u]
            [jsk.data.crud :as crud]
            [jsk.data.schedule :as schedule]
            [e85th.commons.ex :as ex]))

(use-fixtures :once test/with-system)

(deftest cron-expr-test
  (testing "invalid cron exprs"
    (is (false?  (schedule/cron-expr? nil)))
    (is (false?  (schedule/cron-expr? "")))
    (is (false?  (schedule/cron-expr? "invalid-expr"))))

  (testing "valid cron exprs"
    (is (schedule/cron-expr? "0 0 12 ? * WED"))))

(deftest validate-cron-expr-test
  (is (thrown? Exception (schedule/validate-cron-expr nil))))

(deftest ^:integration crud-test
  (let [{schedule-id :db/id} (crud/create-schedule)]
    ;; -- update
    (let [update-data {:schedule/name (u/uuid) :schedule/cron-expr "0 0 12 ? * THU"}
          updated-schedule (crud/update-schedule update-data schedule-id)]
      (is (= (select-keys updated-schedule [:db/id :schedule/name :schedule/cron-expr])
             (assoc update-data :db/id schedule-id))))

    (crud/get-schedule schedule-id)
    (crud/delete-schedule schedule-id)))

(deftest ^:integration create-fails-with-bad-data
  (let [data {:schedule/cron-expr "invalid-expr" :schedule/name (u/uuid)}
        {:keys [errors]} (crud/create-schedule 422 data)]
    (is (seq errors))))

(deftest ^:integration update-fails-with-bad-data
  (let [{schedule-id :db/id :as schedule} (crud/create-schedule)
        {:keys [errors]} (crud/update-schedule 422 (assoc schedule :schedule/cron-expr "invalid-expr") schedule-id)]
    (is (seq errors))))

;; (deftest ^:integration schedule-workflow-assoc-test
;;   (let [{schedule-id :id} (crud/create-schedule)
;;         {workflow-id :id} (crud/create-workflow)
;;         sched-wf-assoc (crud/create-schedule-workflow-assoc schedule-id workflow-id)]
;;     (crud/delete-schedule-workflow-assoc (:id sched-wf-assoc))))

;; (deftest ^:integration schedule-job-assoc-test
;;   (let [{schedule-id :id} (crud/create-schedule)
;;         {job-id :id} (crud/create-job)
;;         sched-job-assoc (crud/create-schedule-job-assoc schedule-id job-id)]
;;     (crud/delete-schedule-job-assoc (:id sched-job-assoc))))
