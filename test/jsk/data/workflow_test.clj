(ns jsk.data.workflow-test
  (:require [clojure.test :refer :all]
            [jsk.core-test :as test]
            [taoensso.timbre :as log]
            [jsk.data.crud :as crud]
            [e85th.commons.util :as u]))

(use-fixtures :once test/with-system)

(deftest ^:integration crud-test

  ;; -- create
  (let [{workflow-id :id} (crud/create-workflow)]
    ;; -- update
    (let [update-data {:name  (u/uuid) :is-enabled false}
          workflow (crud/update-workflow 200 update-data workflow-id)]
      (is (= (assoc update-data :id workflow-id)
             (select-keys workflow [:id :is-enabled :name]))))

    (crud/get-workflow workflow-id)
    (crud/delete-workflow workflow-id)))
