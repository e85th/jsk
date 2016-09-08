(ns jsk.data.alert
  (:require [jsk.data.db :as db]
            [jsk.data.models :as m]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [e85th.commons.ex :as ex]))

(s/defn find-by-id :- (s/maybe m/Alert)
  "Finds an alert by id."
  [{:keys [db]} alert-id :- s/Int]
  (db/select-alert-by-id db alert-id))

(def ^{:doc "Same as find-by-id except throws NotFoundException if no such alert."}
  find-by-id! (ex/wrap-not-found find-by-id))

(s/defn create :- m/Alert
  "Creates a new alert."
  [{:keys [db] :as res} new-alert :- m/AlertCreate user-id :- s/Int]
  (let [alert-id (db/insert-alert db new-alert user-id)]
    (log/infof "Created new alert with id %s" alert-id)
    (find-by-id res alert-id)))

(s/defn amend :- m/Alert
  "Updates and returns the new record."
  [{:keys [db] :as res} alert-id :- s/Int alert-update :- m/AlertAmend user-id :- s/Int]
  (db/update-alert db alert-id alert-update user-id)
  (find-by-id! res alert-id))

(s/defn delete
  "Delete an alert."
  [{:keys [db]} alert-id :- s/Int user-id :- s/Int]
  (db/delete-alert db alert-id))
