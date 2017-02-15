(ns jsk.data.workflow-test
  (:require [clojure.test :refer :all]
            [jsk.core-test :as test]
            [taoensso.timbre :as log]
            [jsk.data.crud :as crud]
            [e85th.commons.util :as u]))

(use-fixtures :once test/with-system)

(deftest ^:integration crud-test

  ;; -- create
  (let [{workflow-id :db/id :as wf} (crud/create-workflow)]
    ;; -- update
    (let [update-data (assoc wf :workflow/name (u/uuid) :workflow/enabled? false)
          wf (crud/update-workflow 200 update-data workflow-id)]
      (is (= update-data wf)))

    (crud/get-workflow workflow-id)
    (crud/delete-workflow workflow-id)))
