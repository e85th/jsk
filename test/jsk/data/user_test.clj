(ns jsk.data.user-test
  (:require [clojure.test :refer :all]
            ;[jsk.core-test :as test]
            [taoensso.timbre :as log]
            [datomic.api :as d]
            [jsk.data.user :as user]
            [e85th.commons.datomic :as datomic]
            [e85th.commons.util :as u]))

;(use-fixtures :once test/with-system)
(def uri "datomic:mem://scheduler-test")

(defn create-empty-in-memory-db
  ([]
   (create-empty-in-memory-db (load-file "resources/schema.edn")))
  ([schema]
   (d/delete-database uri)
   (d/create-database uri)
   (let [cn (d/connect uri)]
     @(d/transact cn schema)
     cn)))


(deftest find-user-test
  (let [cn (create-empty-in-memory-db)
        res {:db {:cn cn}}
        channels [{:channel/type :email
                   :channel/identifier "john@example.com"}]
        user {:user/first-name "John"
              :user/last-name "Doe"
              :user/channels channels
              :user/password "foo"}
        {:keys [:db/id]} (user/create-new-user res user 1)
        found (user/find-user-by-id res id)]
    (is (= found
           (user/find-user-by-identifier res "john@example.com")))))


;; (def cn (create-empty-in-memory-db))

;; (def res {:db {:cn cn}})

;; (def channels [{:channel/type :email
;;                 :channel/identifier "john@example.com"}])
;; (def u-entity {:user/first-name "John"
;;                :user/last-name "Doe"
;;                :user/channels channels
;;                :user/password "foo"})

;; (def u (user/create-new-user res u-entity 1))

;; (user/find-user-auth res (:db/id u))


;; (user/find-channel-by-id res 277076930200555),

;; (user/find-channels-by-identifier res "john@example.com")

;;(user/find-user-by-identifier! res "john@example.com")

;;(user/rm-user res (:db/id u) 1)


;; (user/find-channel-by-type res :email "john@example.com")

;; (user/find-user-id-by-identifier res  "john@example.com")

;; (def new-c (user/create-channel res {:channel/type :mobile
;;                                      :channel/identifier "+19277249558"} (:db/id u)))

;; new-c
;; (user/update-channel res (:db/id new-c) {:channel/identifier "+12121234567"} 1)

;; (user/remove-channel res (:db/id new-c) 1)

;; (user/filter-existing-identifiers res [
;;                                        {:channel/identifier "john@example.com"}
;;                                        {:channel/identifier "+19177249558"}
;;                                        {:channel/identifier "+12121234567"}
;;                                        ])
