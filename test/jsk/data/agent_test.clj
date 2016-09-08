(ns jsk.data.agent-test
  (:require [clojure.test :refer :all]
            [jsk.core-test :as test]
            [taoensso.timbre :as log]
            [e85th.commons.util :as u]))

(use-fixtures :once test/with-system)

(def base-end-point "/api/v1/agent")

(deftest ^:integration crud-test

  ;; -- create
  (let [agent-name (u/uuid)
        [status response] (test/api-call :post base-end-point {:name agent-name})
        agent-id (:id response)
        agent-end-point (str base-end-point "/" agent-id)]
    (is (= 201 status))
    (is (= agent-name (:name response)))

    ;; -- update
    (let [new-name (u/uuid)
          [status response] (test/api-call :put agent-end-point {:name new-name})]
      (is (= 200 status))
      (is (= agent-id (:id response)))
      (is (= new-name (:name response))))

    ;; -- get
    (let [[status response] (test/api-call :get agent-end-point)]
      (is (= 200 status))
      (is (= agent-id (:id response))))

    ;; -- delete
    (let [[status response] (test/api-call :delete agent-end-point)]
      (is (= 200 status))
      (is (= agent-id (:id response))))))
