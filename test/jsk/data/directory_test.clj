(ns jsk.data.directory-test
  (:require [clojure.test :refer :all]
            [jsk.core-test :as test]
            [taoensso.timbre :as log]
            [e85th.commons.util :as u]
            [jsk.data.directory :as dir]))

(use-fixtures :once test/with-system)

(def base-end-point "/api/v1/directory")

(deftest ^:integration crud-test
  ;; -- create
  (let [directory-name (u/uuid)
        [status response] (test/api-call :post base-end-point {:name directory-name
                                                               :parent-id dir/root-dir-id})
        directory-id (:id response)
        directory-end-point (str base-end-point "/" directory-id)]
    (is (= 201 status))
    (is (= directory-name (:name response)))

    ;; -- update
    (let [new-name (u/uuid)
          [status response] (test/api-call :put directory-end-point {:name new-name})]
      (is (= 200 status))
      (is (= directory-id (:id response)))
      (is (= new-name (:name response))))

    ;; -- get
    (let [[status response] (test/api-call :get directory-end-point)]
      (is (= 200 status))
      (is (= directory-id (:id response))))

    ;; -- delete
    (let [[status response] (test/api-call :delete directory-end-point)]
      (is (= 200 status))
      (is (= directory-id (:id response))))))
