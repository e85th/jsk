(ns dev
  (:refer-clojure :exclude [test])
  (:require [clojure.repl :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [e85th.commons.datomic :as datomic]
            [jsk.data.user :as user]
            [jsk.main :as main]
            [reloaded.repl :refer [system init start stop go reset]]))

(reloaded.repl/set-init! main/make-system)

;; (def uri "datomic:mem://scheduler-test")
;; (defn create-empty-in-memory-db
;;   ([]
;;    (create-empty-in-memory-db (load-file "resources/schema.edn")))
;;   ([schema]
;;    (d/delete-database uri)
;;    (d/create-database uri)
;;    (let [cn (d/connect uri)]
;;      @(d/transact cn schema)
;;      cn)))

;; (def cn (create-empty-in-memory-db))

;; (def res {:db {:cn cn}})

;; (def channels [{:channel/type :email
;;                 :channel/identifier "john@example.com"}])

;; (def u-entity {:user/first-name "John"
;;                :user/last-name "Doe"
;;                :user/channels channels
;;                :user/password "foo"})

;; (def u (user/create-new-user res u-entity 1))

;; (user/add-user-roles res (:db/id u) {:role/name :foo} 1)

;; (first (get-in (user/find-user-by-id! res (:db/id u))
;;                [:user/roles]))

;; (user/rm-user res (:db/id u) 1)

;; (d/touch (d/entity (d/db cn) 23))
