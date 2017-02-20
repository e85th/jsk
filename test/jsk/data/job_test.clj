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
