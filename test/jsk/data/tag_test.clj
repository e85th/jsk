(ns jsk.data.tag-test
  (:require [jsk.core-test :as test]
            [taoensso.timbre :as log]
            [e85th.commons.util :as u]
            [jsk.data.crud :as crud]
            [clojure.test :refer :all]))


(use-fixtures :once test/with-system)

(deftest ^:integration crud-test
  (let [{tag-id :db/id} (crud/create-tag)]
    ;; -- update
    (let [update-data {:tag/name (u/uuid)}
          updated-tag (crud/update-tag update-data tag-id)]
      (is (= (select-keys updated-tag [:db/id :tag/name])
             (assoc update-data :db/id tag-id))))

    (crud/get-tag tag-id)

    ;; -- get all
    (let [[status tags] (test/api-call :get crud/tag-base-endpoint)]
      (is (= 200 status))
      (is (pos? (count tags)))
      (is (every? :tag/name tags)))


    (crud/delete-tag tag-id)))
