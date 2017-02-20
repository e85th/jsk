(ns jsk.data.tag-test
  (:require [jsk.core-test :as test]
            [taoensso.timbre :as log]
            [e85th.commons.util :as u]
            [clojure.test :refer :all]))


(use-fixtures :once test/with-system)

(def base-endpoint "/api/v1/tags")

(deftest ^:integration crud-test

  ;; -- create
  (let [tag-name (u/uuid)
        [status response] (test/api-call :post base-endpoint {:tag/name tag-name})
        tag-id (:db/id response)
        tag-endpoint (str base-endpoint "/" tag-id)]
    (is (= 201 status))
    (is (= tag-name (:tag/name response)))

    ;; -- update
    (let [new-name (u/uuid)
          [status response] (test/api-call :put tag-endpoint {:tag/name new-name})]
      (is (= 200 status))
      (is (= tag-id (:db/id response)))
      (is (= new-name (:tag/name response))))

    ;; -- get
    (let [[status response] (test/api-call :get tag-endpoint)]
      (is (= 200 status))
      (is (= tag-id (:db/id response))))


    ;; -- get all
    (let [[status tags] (test/api-call :get base-endpoint)]
      (is (= 200 status))
      (is (pos? (count tags)))
      (is (every? :tag/name tags)))

    ;; -- delete
    (let [[status response] (test/api-call :delete tag-endpoint)]
      (is (= 200 status))
      (is (= tag-id (:db/id response))))))
