(ns jsk.data.agent
  (:require [jsk.data.db :as db]
            [jsk.data.models :as m]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [datomic.api :as d]
            [e85th.commons.datomic :as datomic]
            [e85th.commons.mq :as mq]
            [e85th.commons.ex :as ex]))

(s/defn find-all :- [m/Agent]
  [{:keys [db]}]
  (datomic/get-all-entities-with-attr db :agent/name))

(s/defn find-by-id :- (s/maybe m/Agent)
  "Finds an agent by id."
  [{:keys [db]} agent-id :- s/Int]
  (datomic/get-entity-with-attr db agent-id :agent/name))

(def ^{:doc "Same as find-by-id except throws NotFoundException if no such agent."}
  find-by-id! (ex/wrap-not-found find-by-id))

(s/defn create :- m/Agent
  "Creates a new agent."
  [{:keys [db publisher] :as res} agent :- m/Agent user-id :- s/Int]
  (let [temp-id (d/tempid :db.part/jsk)
        facts [(assoc agent :db/id temp-id)]
        {:keys [db-after tempids]} @(d/transact (:cn db) facts)
        agent-id (d/resolve-tempid db-after tempids temp-id)]
    (mq/publish publisher [:jsk.agent/created agent-id])
    (find-by-id res agent-id)))

(s/defn modify :- m/Agent
  "Updates and returns the new record."
  [{:keys [db publisher] :as res} agent-id :- s/Int agent :- m/Agent user-id :- s/Int]
  @(d/transact (:cn db) [(assoc agent :db/id agent-id)])
  (mq/publish publisher [:jsk.agent/modified agent-id])
  (find-by-id! res agent-id))

(s/defn rm
  "Delete an agent."
  [{:keys [db publisher]} agent-id :- s/Int user-id :- s/Int]
  @(d/transact (:cn db) [[:db/retractEntity agent-id]])
  (mq/publish publisher [:jsk.agent/deleted agent-id]))
