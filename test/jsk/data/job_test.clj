(ns jsk.data.job-types-test
  (:require [clojure.test :refer :all]
            [jsk.core-test :as test]
            [jsk.data.crud :as crud]
            [taoensso.timbre :as log]
            [e85th.commons.util :as u]
            [jsk.data.models :as m]))

(use-fixtures :once test/with-system)

(deftest ^:integration fetch-test
  (let [[status job-type->schema] (test/api-call :get "/api/v1/job-types")]
    (is (= 200 status))
    (is (some? (:shell job-type->schema)))))

(deftest ^:integration job-schedule-test
  (let [{schedule-id :db/id} (crud/create-schedule)
        {job-id :db/id} (crud/create-job)]
    (testing "job schedule assoc"
      (crud/assoc-job-schedule job-id schedule-id))
    (testing "job schedule dissoc"
      (crud/dissoc-job-schedule job-id schedule-id))))

(deftest ^:integration job-alert-test
  (let [{alert-id :db/id} (crud/create-alert)
        {job-id :db/id} (crud/create-job)]
    (testing "job alert assoc"
      (crud/assoc-job-alert job-id alert-id))
    (testing "job alert dissoc"
      (crud/dissoc-job-alert job-id alert-id))))

(deftest ^:integration job-tag-test
  (let [{tag-id :db/id} (crud/create-tag)
        {job-id :db/id} (crud/create-job)]
    (testing "job tag assoc"
      (crud/assoc-job-tag job-id tag-id))
    (testing "job tag dissoc"
      (crud/dissoc-job-tag job-id tag-id))))
