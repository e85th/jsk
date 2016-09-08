(ns jsk.data.agent
  (:require [jsk.data.db :as db]
            [jsk.data.models :as m]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [e85th.commons.ex :as ex]))

(s/defn find-by-id :- (s/maybe m/Agent)
  "Finds an agent by id."
  [{:keys [db]} agent-id :- s/Int]
  (db/select-agent-by-id db agent-id))

(def ^{:doc "Same as find-by-id except throws NotFoundException if no such agent."}
  find-by-id! (ex/wrap-not-found find-by-id))

(s/defn create :- m/Agent
  "Creates a new agent."
  [{:keys [db] :as res} new-agent :- m/AgentCreate user-id :- s/Int]
  (let [agent-id (db/insert-agent db new-agent user-id)]
    (log/infof "Created new agent with id %s" agent-id)
    (find-by-id res agent-id)))

(s/defn amend :- m/Agent
  "Updates and returns the new record."
  [{:keys [db] :as res} agent-id :- s/Int agent-update :- m/AgentAmend user-id :- s/Int]
  (db/update-agent db agent-id agent-update user-id)
  (find-by-id! res agent-id))

(s/defn delete
  "Delete an agent."
  [{:keys [db]} agent-id :- s/Int user-id :- s/Int]
  (db/delete-agent db agent-id))
