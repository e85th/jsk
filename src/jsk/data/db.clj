(ns jsk.data.db
  (:require [schema.core :as s]
            [jsk.data.models :as m]
            [clojure.set :as set]
            [datomic.api :as d]
            [taoensso.timbre :as log]))

(s/defn find-channel-by-identifier
  [db identifier :- s/Str]
  (ffirst (d/q '[:find (pull ?eid [*] ...)
                 :in $ ?id
                 :where [?eid :channel/identifier ?id]]
               (-> db :cn d/db)
               identifier)))

(s/defn find-channel-by-type :- (s/maybe m/Channel)
  [db channel-type :- s/Keyword identifier :- s/Str]
  (ffirst (d/q '[:find (pull ?eid [*] ...)
                 :in $ ?type ?id
                 :where [?eid :channel/identifier ?id]
                 [?eid :channel/type ?type]]
               (-> db :cn d/db)
               channel-type
               identifier)))

(s/defn find-user-id-by-identifier :- (s/maybe s/Int)
  [db identifier :- s/Str]
  (ffirst (d/q '[:find ?uid
                 :in $ ?id
                 :where [?cid :channel/identifier ?id]
                        [?uid :user/channels ?cid]]
               (-> db :cn d/db)
               identifier)))

(s/defn filter-existing-identifiers :- [s/Str]
  "Returns only identifiers which exist."
  [db channels :- [m/Channel]]
  (d/q '[:find [?id ...]
         :in $ [?id ...]
         :where [_ :channel/identifier ?id]]
       (-> db :cn d/db)
       (mapv :channel/identifier channels)))

(s/defn find-user-ids-by-roles :- [s/Int]
  [db roles :- [s/Keyword]]
  (d/q '[:find [?uid ...]
         :in $ [?role ...]
         :where [?rid :role/name ?role]
                [?uid :user/roles ?rid]]
       (-> db :cn d/db)
       roles))


(s/defn find-tag-ids :- [s/Int]
  [db tag-names :- [s/Str]]
  (d/q '[:find [?tid ...]
         :in $ [?tag ...]
         :where [?tid :tag/name ?tag]]
       (-> db :cn d/db)
       tag-names))

(s/defn find-all-tag-ids :- [s/Int]
  [db]
  (d/q '[:find [?tid ...]
         :where [?tid :tag/name]]
       (-> db :cn d/db)))

(s/defn find-tags-by-job-id
  [db job-id :- s/Int]
  (d/q '[:find ?tags
         :in $ ?job-id
         :where [?job-id :job/tags ?tags]]
       (-> db :cn d/db)
       job-id))


(s/defn find-alert-channel-info :- [m/ChannelInfo]
  [db alert-id :- s/Int]
  (let [rs (d/q '[:find ?chan-id ?identifier ?first-name ?last-name
                  :in $ ?alert-id
                  :where [?alert-id :alert/channels ?chan-id]
                  [?chan-id :channel/identifier ?identifier]
                  [?user-id :user/channels ?chan-id]
                  [?user-id :user/first-name ?first-name]
                  [?user-id :user/last-name ?last-name]]
                (-> db :cn d/db)
                alert-id)]
    (map (partial zipmap [:channel/id :channel/identifier :user/first-name :user/last-name]) rs)))


(s/defn find-id-name
  [db field :- s/Keyword ids :- s/Int]
  (d/q '[:find ?id ?name
         :in $ ?field [?id ...]
         :where [?id ?field ?name]]
       (-> db :cn d/db) field ids))
