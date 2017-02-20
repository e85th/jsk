(ns jsk.data.job-test
  (:require [clojure.test :refer :all]
            [jsk.core-test :as test]
            [jsk.data.crud :as crud]
            [taoensso.timbre :as log]
            [e85th.commons.util :as u]
            [jsk.data.models :as m]))

(use-fixtures :once test/with-system)

(deftest ^:integration crud-test
  (let [{job-id :db/id :as job} (crud/create-job)]

    ;; -- update
    (let [update-data (assoc job :job/name (u/uuid) :job/enabled? false)
          job (crud/update-job 200 update-data job-id)]
      (is (= update-data job)))

    (crud/get-job job-id)

    ;; -- get all
    (let [[status jobs] (test/api-call :get crud/job-base-endpoint)]
      (is (= 200 status))
      (is (pos? (count jobs)))
      (is (every? :job/name jobs)))

    (crud/delete-job job-id)))

(deftest ^:integration job-schedule-test
  (let [{schedule-id :db/id} (crud/create-schedule)
        {job-id :db/id} (crud/create-job)]
    (testing "job schedule assoc"
      (crud/assoc-job-schedule job-id schedule-id))
    (testing "job schedule dissoc"
      (crud/dissoc-job-schedule job-id schedule-id))))
