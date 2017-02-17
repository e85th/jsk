(ns jsk.core-test
  (:require [e85th.test.http :as http]
            [e85th.commons.util :as u]
            [jsk.common.conf :as conf]
            [jsk.routes :as routes]
            [com.stuartsierra.component :as component]
            [jsk.test-system :as test-system]
            [jsk.test-data :as test-data]
            [schema.core :as s]
            [clojure.test :refer :all]
            [datomic.api :as d]
            [jsk.data.user :as user]
            [taoensso.timbre :as log]))

(def config-file "config.edn")
(def auth-token-name "Bearer")
(def user-email test-data/user-email)
(def user-password test-data/user-password)
(def admin-user nil)

(defonce system nil)
(defonce admin-user-headers nil)

(defn install-schema
  []
  @(d/transact (get-in system [:db :cn]) (load-file "resources/schema.edn")))

(defn setup-auth
  [res]
  (let [{user-id :db/id} (user/find-user-by-identifier res user-email)
        {:keys [user token]} (user/user-id->auth-response res user-id)]
    (alter-var-root #'admin-user (constantly user))
    (alter-var-root #'admin-user-headers (constantly {http/auth-header (format "%s %s" auth-token-name token)}))))

(defn init!
  "Call this first before using any other functions."
  []
  (u/set-utc-tz)
  (s/set-fn-validation! true)
  (let [sys-config (conf/read-config config-file :test)]
    (alter-var-root #'system (constantly (component/start (test-system/make sys-config))))
    (install-schema)
    (test-data/reset-db! system)
    (setup-auth system)
    (http/init! {:routes (routes/make-handler (:app system))})))


(def api-call (http/make-transit-api-caller (fn [request]
                                              (update-in request [:headers] merge admin-user-headers))))

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
