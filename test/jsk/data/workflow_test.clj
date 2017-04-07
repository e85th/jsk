(ns jsk.data.workflow-test
  (:require [clojure.test :refer :all]
            [jsk.core-test :as test]
            [taoensso.timbre :as log]
            [jsk.data.crud :as crud]
            [jsk.data.workflow :as sut]
            [e85th.commons.util :as u]))

(use-fixtures :once test/with-system)

(deftest ^:integration crud-test

  ;; -- create
  (let [{workflow-id :db/id :as wf} (crud/create-workflow)]
    ;; -- update
    (let [update-data (assoc wf :workflow/name (u/uuid) :workflow/enabled? false)
          wf (crud/update-workflow 200 update-data workflow-id)]
      (is (= update-data wf)))

    (crud/get-workflow workflow-id)

    ;; -- get all
    (let [[status workflows] (test/api-call :get crud/workflow-base-endpoint)]
      (is (= 200 status))
      (is (pos? (count workflows)))
      (is (every? :workflow/name workflows)))

    (crud/delete-workflow workflow-id)))

(deftest ^:integration workflow-schedule-test
  (let [{schedule-id :db/id} (crud/create-schedule)
        {workflow-id :db/id} (crud/create-workflow)]
    (testing "workflow schedule assoc"
      (crud/assoc-workflow-schedule workflow-id schedule-id))
    (testing "workflow schedule dissoc"
      (crud/dissoc-workflow-schedule workflow-id schedule-id))))

(deftest ^:integration workflow-alert-test
  (let [{alert-id :db/id} (crud/create-alert)
        {workflow-id :db/id} (crud/create-workflow)]
    (testing "workflow alert assoc"
      (crud/assoc-workflow-alert workflow-id alert-id))
    (testing "workflow alert dissoc"
      (crud/dissoc-workflow-alert workflow-id alert-id))))

(deftest ^:integration workflow-tag-test
  (let [{tag-id :db/id} (crud/create-tag)
        {workflow-id :db/id} (crud/create-workflow)]
    (testing "workflow tag assoc"
      (crud/assoc-workflow-tag workflow-id tag-id))
    (testing "workflow tag dissoc"
      (crud/dissoc-workflow-tag workflow-id tag-id))))


(deftest normalize-node-inputs-test
  (let [input [{:workflow.node/id "wf-node-9",
                :workflow.node/referent 277076930200565,
                :workflow.node/successors [],
                :workflow.node/successors-err ["wf-node-8"]}
               {:workflow.node/id "wf-node-8",
                :workflow.node/referent 277076930200563,
                :workflow.node/successors [],
                :workflow.node/successors-err []}]
        [n1 n2] (sut/normalize-node-inputs input)]
    (is (= (:db/id n2)
           (first (:workflow.node/successors-err n1))))))
