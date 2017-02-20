(ns jsk.data.agent-test
  (:require [clojure.test :refer :all]
            [jsk.core-test :as test]
            [taoensso.timbre :as log]
            [e85th.commons.util :as u]))

(use-fixtures :once test/with-system)

(def base-endpoint "/api/v1/agents")

(deftest ^:integration crud-test

  ;; -- create
  (let [agent-name (u/uuid)
        [status response] (test/api-call :post base-endpoint {:agent/name agent-name})
        agent-id (:db/id response)
        agent-endpoint (str base-endpoint "/" agent-id)]
    (is (= 201 status))
    (is (= agent-name (:agent/name response)))

    ;; -- update
    (let [new-name (u/uuid)
          [status response] (test/api-call :put agent-endpoint {:agent/name new-name})]
      (is (= 200 status))
      (is (= agent-id (:db/id response)))
      (is (= new-name (:agent/name response))))

    ;; -- get
    (let [[status response] (test/api-call :get agent-endpoint)]
      (is (= 200 status))
      (is (= agent-id (:db/id response))))

    ;; -- get all
    (let [[status agents] (test/api-call :get base-endpoint)]
      (is (= 200 status))
      (is (pos? (count agents)))
      (is (every? :agent/name agents)))

    ;; -- delete
    (let [[status response] (test/api-call :delete agent-endpoint)]
      (is (= 200 status))
      (is (= agent-id (:db/id response))))))
