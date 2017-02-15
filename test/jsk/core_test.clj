(ns jsk.core-test
  (:require [e85th.test.http :as http]
            [e85th.commons.util :as u]
            [jsk.common.conf :as conf]
            [jsk.routes :as routes]
            [com.stuartsierra.component :as component]
            [jsk.test-system :as test-system]
            [schema.core :as s]
            [clojure.test :refer :all]
            [datomic.api :as d]
            [taoensso.timbre :as log]))

(def config-file "config.edn")
(def auth-token-name "Bearer")
(defonce system nil)

(defn install-schema
  []
  @(d/transact (get-in system [:db :cn]) (load-file "resources/schema.edn")))

(defn init!
  "Call this first before using any other functions."
  []
  (u/set-utc-tz)
  (s/set-fn-validation! true)
  (let [sys-config (conf/read-config config-file :test)]
    (alter-var-root #'system (constantly (component/start (test-system/make sys-config))))
    (install-schema)
    (http/init! {:routes (routes/make-handler (:app system))})))


(defn add-admin-auth
  [request]
  (http/add-auth-header request auth-token-name (:admin-auth-token system)))

(def api-call (http/make-transit-api-caller))

(def admin-api-call (http/make-transit-api-caller add-admin-auth))

(defn with-system
  "Runs tests using the test system."
  [f]
  (when (not system)
    (println "Starting test system")
    (init!))
  (f))


(use-fixtures :once with-system)

(comment
  (init!) )
