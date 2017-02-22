(ns jsk.data.search
  (:require [schema.core :as s]
            [jsk.data.models :as m]
            [clojure.set :as set]
            [datomic.api :as d]
            [taoensso.timbre :as log]))

(defn matches?
  [token target]
  (some? (re-find (re-pattern token) target)))

(defn suggest-channels
  [{:keys [db]} token]
  (let [pattern (re-pattern (str ".*" token ".*"))]
    (vec (d/q '[:find ?e ?identifier ?first-name ?last-name
                :in $ ?token
                :where [?e :channel/identifier ?identifier]
                [(jsk.data.search/matches? ?token ?identifier)]
                [?u :user/channels ?e]
                [?u :user/first-name ?first-name]
                [?u :user/last-name ?last-name]]
              (-> db :cn d/db)
              token))))
