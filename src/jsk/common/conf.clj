(ns jsk.common.conf
  (:require [schema.core :as s]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [aero.core :as aero]))

(def config-file "config.edn")

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

(s/defn google-server-key :- s/Str
  [sys-config]
  (get-in sys-config [:google :server-key]))
