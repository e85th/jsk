(ns jsk.data.tag
  (:require [jsk.data.db :as db]
            [jsk.data.models :as m]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [datomic.api :as d]
            [e85th.commons.datomic :as datomic]
            [e85th.commons.ex :as ex]))

(s/defn find-all :- [m/Tag]
  [{:keys [db]}]
  (datomic/get-all-entities-with-attr db :tag/name))

(s/defn find-by-id :- (s/maybe m/Tag)
  "Finds an tag by id."
  [{:keys [db]} tag-id :- s/Int]
  (datomic/get-entity-with-attr db tag-id :tag/name))

(def ^{:doc "Same as find-by-id except throws NotFoundException if no such tag."}
  find-by-id! (ex/wrap-not-found find-by-id))

(s/defn create :- m/Tag
  "Creates a new tag."
  [{:keys [db] :as res} tag :- m/Tag user-id :- s/Int]
  (let [tag-id (d/tempid :db.part/jsk)
        facts [(assoc tag :db/id tag-id)]
        {:keys [db-after tempids]} @(d/transact (:cn db) facts)]
    (find-by-id res (d/resolve-tempid db-after tempids tag-id))))

(s/defn modify :- m/Tag
  "Updates and returns the new record."
  [{:keys [db] :as res} tag-id :- s/Int tag :- m/Tag user-id :- s/Int]
  @(d/transact (:cn db) [(assoc tag :db/id tag-id)])
  (find-by-id! res tag-id))

(s/defn rm
  "Delete an tag."
  [{:keys [db]} tag-id :- s/Int user-id :- s/Int]
  @(d/transact (:cn db) [[:db/retractEntity tag-id]]))


(s/defn find-tag-ids :- [s/Int]
  [{:keys [db]} tag-names :- [s/Str]]
  (db/find-tag-ids db tag-names))

(s/defn ensure-tags :- [s/Int]
  "Makes sure all tag names exist. Answers with the ids of all tags."
  [{:keys [db] :as res} tag-names :- [s/Str]]
  (let [facts (for [x tag-names]
                {:tag/name x})]
    (->> @(d/transact (:cn db) facts)
         :tempids
         vals)))
