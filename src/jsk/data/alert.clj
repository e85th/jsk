(ns jsk.data.alert
  (:require [jsk.data.db :as db]
            [jsk.data.models :as m]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [datomic.api :as d]
            [e85th.commons.datomic :as datomic]
            [e85th.commons.ex :as ex]))

(s/defn find-by-id :- (s/maybe m/Alert)
  "Finds an alert by id."
  [{:keys [db]} alert-id :- s/Int]
  (datomic/get-entity-with-attr db alert-id :alert/name))

(def ^{:doc "Same as find-by-id except throws NotFoundException if no such alert."}
  find-by-id! (ex/wrap-not-found find-by-id))

(s/defn create :- m/Alert
  "Creates a new alert."
  [{:keys [db] :as res} alert :- m/Alert user-id :- s/Int]
  (let [alert-id (d/tempid :db.part/jsk)
        facts [(assoc alert :db/id alert-id)]
        {:keys [db-after tempids]} @(d/transact (:cn db) facts)]
    (find-by-id res (d/resolve-tempid db-after tempids alert-id))))

(s/defn modify :- m/Alert
  "Updates and returns the new record."
  [{:keys [db] :as res} alert-id :- s/Int alert :- m/Alert user-id :- s/Int]
  @(d/transact (:cn db) [(assoc alert :db/id alert-id)])
  (find-by-id! res alert-id))

(s/defn rm
  "Delete an alert."
  [{:keys [db]} alert-id :- s/Int user-id :- s/Int]
  @(d/transact (:cn db) [[:db/retractEntity alert-id]]))


(s/defn add-channels
  [{:keys [db]} alert-id :- s/Int channel-ids :- [s/Int] user-id :- s/Int]
  @(d/transact (:cn db) [[:db/add alert-id :alert/channels channel-ids]]))

(s/defn rm-channels
  [{:keys [db]} alert-id :- s/Int channel-ids :- [s/Int] user-id :- s/Int]
  @(d/transact (:cn db) [[:db/retract alert-id :alert/channels channel-ids]]))
