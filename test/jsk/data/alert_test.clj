(ns jsk.data.alert-test
  (:require [clojure.test :refer :all]
            [jsk.core-test :as test]
            [taoensso.timbre :as log]
            [jsk.data.crud :as crud]
            [e85th.commons.util :as u]))

(use-fixtures :once test/with-system)

(deftest ^:integration crud-test
  (let [{alert-id :db/id} (crud/create-alert)]
    ;; -- update
    (let [update-data {:alert/name (u/uuid)}
          updated-alert (crud/update-alert update-data alert-id)]
      (is (= (select-keys updated-alert [:db/id :alert/name])
             (assoc update-data :db/id alert-id))))

    (crud/get-alert alert-id)

    ;; -- get all
    (let [[status alerts] (test/api-call :get crud/alert-base-endpoint)]
      (is (= 200 status))
      (is (pos? (count alerts)))
      (is (every? :alert/name alerts)))


    (crud/delete-alert alert-id)))
