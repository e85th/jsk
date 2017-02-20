(ns jsk.data.alert-test
  (:require [clojure.test :refer :all]
            [jsk.core-test :as test]
            [taoensso.timbre :as log]
            [e85th.commons.util :as u]))

(use-fixtures :once test/with-system)

(def base-endpoint "/api/v1/alerts")

(deftest ^:integration crud-test

  ;; -- create
  (let [alert-name (u/uuid)
        [status response] (test/api-call :post base-endpoint {:alert/name alert-name
                                                               :alert/desc "Some alert description"})
        alert-id (:db/id response)
        alert-endpoint (str base-endpoint "/" alert-id)]
    (is (= 201 status))
    (is (= alert-name (:alert/name response)))

    ;; -- update
    (let [new-name (u/uuid)
          new-desc "Updated desc"
          [status response] (test/api-call :put alert-endpoint {:alert/name new-name
                                                                 :alert/desc new-desc})]
      (is (= 200 status))
      (is (= alert-id (:db/id response)))
      (is (= new-desc (:alert/desc response)))
      (is (= new-name (:alert/name response))))

    ;; -- get
    (let [[status response] (test/api-call :get alert-endpoint)]
      (is (= 200 status))
      (is (= alert-id (:db/id response))))

    ;; -- get all
    (let [[status alerts] (test/api-call :get base-endpoint)]
      (is (= 200 status))
      (is (pos? (count alerts)))
      (is (every? :alert/name alerts)))

    ;; -- delete
    (let [[status response] (test/api-call :delete alert-endpoint)]
      (is (= 200 status))
      (is (= alert-id (:db/id response))))))
