(ns jsk.data.search
  (:require [schema.core :as s]
            [jsk.data.models :as m]
            [clojure.set :as set]
            [datomic.api :as d]
            [taoensso.timbre :as log]
            [clojure.string :as str]))

(defn matches?
  [token & targets]
  (let [pattern (-> token str/lower-case re-pattern)
        targets (map str/lower-case targets)
        pred? (partial re-find pattern)]
    (some? (some pred? targets))))

(defn suggest-channels
  [{:keys [db]} token]
  (let [pattern (re-pattern (str ".*" token ".*"))]
    (vec (d/q '[:find ?e ?identifier ?first-name ?last-name
                :in $ ?token
                :where [?e :channel/identifier ?identifier]
                [?u :user/channels ?e]
                [?u :user/first-name ?first-name]
                [?u :user/last-name ?last-name]
                [(jsk.data.search/matches? ?token ?identifier ?first-name ?last-name)]]
              (-> db :cn d/db)
              token))))

(defn jobs-by-tag
  [{:keys [db]}]
  (d/q '[:find ?je ?job-name ?we ?wf-name
         :where
         (or-join [?je ?job-name ?we ?wf-name]
                  [?je :job/name ?job-name]
                  [?we :workflow/name ?wf-name])]
       (-> db :cn d/db)))
