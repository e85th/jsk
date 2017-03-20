(ns jsk.data.explorer-test
  (:require [clojure.test :refer :all]
            [jsk.core-test :as test]
            [taoensso.timbre :as log]
            [e85th.commons.util :as u]))

(use-fixtures :once test/with-system)

(def base-endpoint "/api/v1/explorer")

(deftest ^:integration explorer-test

  ;; root
  (let [[status response] (test/api-call :get base-endpoint {:type "alert"})]
    (is (= 200 status))))
