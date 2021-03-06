(ns jsk.common.conf
  (:require [schema.core :as s]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [e85th.commons.util :as u]
            [aero.core :as aero]))

(def config-file "config.edn")
(def jsk-token-name :jsk-token)

;; console to director
;; agents to director
;; address and queue
(def director-inbound-address "jsk.director.inbound")

;; director to agents
;; address and queue
(def agent-inbound-address "jsk.agent.<agent-id>.inbound")

;; director to console topic -- non durable
(def console-inbound-address "jsk.console.inbound")
;; create temporary queues with above address and unique queue name

(def env-name->profile
  {:production :prd
   :staging :stg
   :test :tst
   :development :dev})

(s/defn read-config
  "Reads a config file from the classpath."
  ([env-name :- s/Keyword]
   (read-config config-file env-name))
  ([file :- s/Str env-name :- s/Keyword]
   (let [profile (env-name->profile env-name)]
     (assert profile)
     (-> (aero/read-config (io/resource file) {:profile profile})
         (dissoc :envs :secrets)
         (assoc :env (name env-name))))))


(s/defn datomic-uri :- s/Str
  [sys-config]
  (get-in sys-config [:datomic-uri]))

(s/defn log-file :- s/Str
  [sys-config]
  (get-in sys-config [:log-file]))

(s/defn auth-secret :- s/Str
  [sys-config]
  (get-in sys-config [:jsk-token :secret]))

(s/defn auth-token-ttl-minutes
  [sys-config]
  (get-in sys-config [:jsk-token :ttl] 600))

(s/defn ^:dev-only google-auth-settings
  [sys-config]
  (get-in sys-config [:settings :auth :google]))

(s/defn ^:dev-only smtp-settings
  [sys-config]
  (get-in sys-config [:settings :smtp]))

(s/defn ^:dev-only site-settings
  [sys-config]
  (get-in sys-config [:settings :site]))
