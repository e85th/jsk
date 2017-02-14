(ns jsk.data.tag-test
  (:require [jsk.core-test :as test]
            [taoensso.timbre :as log]
            [e85th.commons.util :as u]
            [clojure.test :refer :all]))


(use-fixtures :once test/with-system)

(def base-end-point "/api/v1/tag")

(deftest ^:integration crud-test

  ;; -- create
  (let [tag-name (u/uuid)
        [status response] (test/api-call :post base-end-point {:tag/name tag-name})
        tag-id (:db/id response)
        tag-end-point (str base-end-point "/" tag-id)]
    (is (= 201 status))
    (is (= tag-name (:tag/name response)))

    ;; -- update
    (let [new-name (u/uuid)
          [status response] (test/api-call :put tag-end-point {:tag/name new-name})]
      (is (= 200 status))
      (is (= tag-id (:db/id response)))
      (is (= new-name (:tag/name response))))

    ;; -- get
    (let [[status response] (test/api-call :get tag-end-point)]
      (is (= 200 status))
      (is (= tag-id (:db/id response))))

    ;; -- delete
    (let [[status response] (test/api-call :delete tag-end-point)]
      (is (= 200 status))
      (is (= tag-id (:db/id response))))))
