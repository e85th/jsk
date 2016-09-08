(ns jsk.data.job-test
  (:require [clojure.test :refer :all]
            [jsk.core-test :as test]
            [jsk.data.crud :as crud]
            [taoensso.timbre :as log]
            [e85th.commons.util :as u]
            [jsk.data.models :as m]))

(use-fixtures :once test/with-system)

(deftest ^:integration crud-test
  (let [{job-id :id} (crud/create-job)]

    ;; -- update
    (let [update-data {:name (u/uuid) :is-enabled false}
          job (crud/update-job 200 update-data job-id)]
      (is (= (assoc update-data :id job-id)
             (select-keys job [:id :is-enabled :name]))))

    (crud/get-job job-id)
    (crud/delete-job job-id)))
