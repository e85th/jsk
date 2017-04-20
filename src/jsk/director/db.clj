(ns jsk.director.db
  (:require [schema.core :as s]
            [jsk.director.models :as dm]
            [clojure.set :as set]
            [datomic.api :as d]
            [taoensso.timbre :as log]))

(s/defn find-node-schedules :- dm/NodeSchedules
  [db]
  (let [rs (d/q '[:find ?id ?cron-expr
                  :in $ [?attr ...]
                  :where [?id ?attr ?sched-id]
                  [?sched-id :schedule/cron-expr ?cron-expr]]
                (-> db :cn d/db)
                [:job/schedules :workflow/schedules])]
    (reduce (fn [m [k v]]
              (update-in m [k] (fnil conj #{}) v))
            {}
            rs)))
