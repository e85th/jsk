(ns jsk.data.alert-test
  (:require [clojure.test :refer :all]
            [jsk.core-test :as test]
            [taoensso.timbre :as log]
            [e85th.commons.util :as u]))

(use-fixtures :once test/with-system)

(def base-end-point "/api/v1/alert")

(deftest ^:integration crud-test

  ;; -- create
  (let [alert-name (u/uuid)
        [status response] (test/api-call :post base-end-point {:alert/name alert-name
                                                               :alert/desc "Some alert description"})
        alert-id (:db/id response)
        alert-end-point (str base-end-point "/" alert-id)]
    (is (= 201 status))
    (is (= alert-name (:alert/name response)))

    ;; -- update
    (let [new-name (u/uuid)
          new-desc "Updated desc"
          [status response] (test/api-call :put alert-end-point {:alert/name new-name
                                                                 :alert/desc new-desc})]
      (is (= 200 status))
      (is (= alert-id (:db/id response)))
      (is (= new-desc (:alert/desc response)))
      (is (= new-name (:alert/name response))))

    ;; -- get
    (let [[status response] (test/api-call :get alert-end-point)]
      (is (= 200 status))
      (is (= alert-id (:db/id response))))

    ;; -- delete
    (let [[status response] (test/api-call :delete alert-end-point)]
      (is (= 200 status))
      (is (= alert-id (:db/id response))))))
