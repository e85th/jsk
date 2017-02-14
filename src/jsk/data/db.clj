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
  (d/q '[:find (pull ?eid [*] ...)
         :in $ ?type ?id
         :where [?eid :channel/identifier ?id]
         [?eid :channel/type ?type]]
       (-> db :cn d/db)
       channel-type
       identifier))

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

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ;; Agent
;; (s/defn insert-agent :- s/Int
;;   [db new-agent :- m/AgentCreate user-id :- s/Int]
;;   (:id (sql/insert! db :agent new-agent user-id)))

;; (s/defn update-agent :- s/Int
;;   [db agent-id :- s/Int row :- m/AgentAmend user-id :- s/Int]
;;   (sql/update! db :agent row ["id = ?" agent-id] user-id))

;; (s/defn delete-agent
;;   "Delete an agent by id."
;;   [db agent-id :- s/Int]
;;   (sql/delete! db :agent ["id = ?" agent-id]))

;; ;; -- Agent Select
;; (def ^:private default-agent-params
;;   {:id-nil? true :id nil
;;    :name-nil? true :name nil})

;; (s/defn ^:private select-agent*
;;   [db m]
;;   (select-agent db (merge default-agent-params m)))

;; (s/defn select-agent-by-id :- (s/maybe m/Agent)
;;   "Select an agent by id."
;;   [db id :- s/Int]
;;   (first (select-agent* db {:id-nil? false :id id})))

;; (s/defn select-agent-by-name :- (s/maybe m/Agent)
;;   "Select an agent by name s."
;;   [db s :- s/Str]
;;   (first (select-agent* db {:name-nil? false :name s})))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ;; Alert
;; (s/defn insert-alert :- s/Int
;;   [db new-alert :- m/AlertCreate user-id :- s/Int]
;;   (:id (sql/insert! db :alert new-alert user-id)))

;; (s/defn update-alert :- s/Int
;;   [db alert-id :- s/Int row :- m/AlertAmend user-id :- s/Int]
;;   (sql/update! db :alert row ["id = ?" alert-id] user-id))

;; (s/defn delete-alert
;;   "Delete an alert by id."
;;   [db alert-id :- s/Int]
;;   (sql/delete! db :alert ["id = ?" alert-id]))

;; ;; -- Alert Select
;; (def ^:private default-alert-params
;;   {:id-nil? true :id nil
;;    :name-nil? true :name nil})

;; (s/defn ^:private select-alert*
;;   [db m]
;;   (select-alert db (merge default-alert-params m)))

;; (s/defn select-alert-by-id :- (s/maybe m/Alert)
;;   "Select an alert by id."
;;   [db id :- s/Int]
;;   (first (select-alert* db {:id-nil? false :id id})))

;; (s/defn select-alert-by-name :- (s/maybe m/Alert)
;;   "Select an alert by name s."
;;   [db s :- s/Str]
;;   (first (select-alert* db {:name-nil? false :name s})))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ;; Schedule
;; (s/defn insert-schedule :- s/Int
;;   [db new-schedule :- m/ScheduleCreate user-id :- s/Int]
;;   (:id (sql/insert! db :schedule new-schedule user-id)))

;; (s/defn update-schedule :- s/Int
;;   [db schedule-id :- s/Int row :- m/ScheduleAmend user-id :- s/Int]
;;   (sql/update! db :schedule row ["id = ?" schedule-id] user-id))

;; (s/defn delete-schedule
;;   "Delete an schedule by id."
;;   [db schedule-id :- s/Int]
;;   (sql/delete! db :schedule ["id = ?" schedule-id]))

;; ;; -- Schedule Select
;; (def ^:private default-schedule-params
;;   {:id-nil? true :id nil
;;    :name-nil? true :name nil})

;; (s/defn ^:private select-schedule*
;;   [db m]
;;   (select-schedule db (merge default-schedule-params m)))

;; (s/defn select-schedule-by-id :- (s/maybe m/Schedule)
;;   "Select an schedule by id."
;;   [db id :- s/Int]
;;   (first (select-schedule* db {:id-nil? false :id id})))

;; (s/defn select-schedule-by-name :- (s/maybe m/Schedule)
;;   "Select an schedule by name s."
;;   [db s :- s/Str]
;;   (first (select-schedule* db {:name-nil? false :name s})))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ;; Directory
;; (s/defn insert-directory :- s/Int
;;   [db new-directory :- m/DirectoryCreate user-id :- s/Int]
;;   (:id (sql/insert! db :directory new-directory user-id)))

;; (s/defn update-directory :- s/Int
;;   [db dir-id :- s/Int row :- m/DirectoryAmend user-id :- s/Int]
;;   (sql/update! db :directory row ["id = ?" dir-id] user-id))

;; (s/defn delete-directory
;;   "Delete an directory by id."
;;   [db dir-id :- s/Int]
;;   (sql/delete! db :directory ["id = ?" dir-id]))

;; ;; -- Directory Select
;; (def ^:private default-directory-params
;;   {:id-nil? true :id nil
;;    :name-nil? true :name nil
;;    :parent-id-nil? true :parent-id nil
;;    :root-nil? true})

;; (s/defn ^:private select-directory*
;;   [db m]
;;   (select-directory db (merge default-directory-params m)))

;; (s/defn select-directory-by-id :- (s/maybe m/Directory)
;;   [db dir-id :- s/Int]
;;   (first (select-directory* db {:id-nil? false :id dir-id})))

;; (s/defn select-immediate-child-dirs :- [m/Directory]
;;   [db parent-id :- (s/maybe s/Int)]
;;   (let [params (if (some? parent-id)
;;                  {:parent-id-nil? false :parent-id parent-id}
;;                  {:root-nil? false})]
;;     (select-directory* db params)))


;; (s/defn select-directory-contents-by-dir-id :- [m/DirectoryItem]
;;   [db dir-id :- s/Int]
;;   (map #(update-in % [:type] keyword)
;;        (select-directory-contents db {:directory-id dir-id})))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ;; Node
;; (def ^:private node-table-keys [:name :description :is-enabled :tags :node-type-id])

;; (s/defn ^:private insert-node :- s/Int
;;   [txn node user-id :- s/Int]
;;   (let [node (select-keys node node-table-keys)]
;;     (:id (sql/insert! txn :node node user-id))))

;; (s/defn ^:private update-node
;;   [txn node-id :- s/Int node user-id :- s/Int]
;;   (let [keys-present (vec (set/intersection (set (keys node)) (set node-table-keys)))
;;         node (select-keys node keys-present)]
;;     (sql/update! txn :node node ["id = ?" node-id] user-id)))

;; (s/defn ^:private delete-node
;;   [txn node-id :- s/Int]
;;   (sql/delete! txn :node ["id = ?" node-id]))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ;; Job
;; (def ^:private default-job-params
;;   {:id-nil? true :id nil})

;; (s/defn ^:private select-job*
;;   [db m]
;;   (select-job db (merge default-job-params m)))

;; (s/defn select-job-by-id :- (s/maybe m/Job)
;;   "Select an job by id."
;;   [db id :- s/Int]
;;   (first (select-job* db {:id-nil? false :id id})))

;; (s/defn job-id->node-id :- s/Int
;;   [db job-id :- s/Int]
;;   (let [row (first (sql/query db ["select node_id as id from job where id = ?" job-id]))]
;;     (assert row)
;;     (:id row)))

;; (s/defn node-id->job-id :- s/Int
;;   [db node-id :- s/Int]
;;   (let [{:keys [node-type-id id]} (select-node-id->child-id db {:node-id node-id})]
;;     (assert (= node-type-id m/job-node-type-id))
;;     id))

;; (def ^:private job-table-keys [:agent-id :node-id :type :props :behavior-id :context-key :max-retries :max-concurrent :timeout])

;; (s/defn insert-job :- s/Int
;;   [db new-job :- m/JobCreate user-id :- s/Int]
;;   (jdbc/with-db-transaction [txn db]
;;     (let [node-id (insert-node txn (assoc new-job :node-type-id m/job-node-type-id) user-id)
;;           new-job* (reduce dissoc (assoc new-job :node-id node-id) node-table-keys)]
;;       (:id (sql/insert! txn :job new-job*)))))

;; (s/defn update-job :- (s/maybe s/Int)
;;   [db job-id :- s/Int row :- m/JobAmend user-id :- s/Int]
;;   (jdbc/with-db-transaction [txn db]
;;     (let [node-id (job-id->node-id txn job-id)
;;           job-keys (vec (set/intersection (set (keys row)) (set job-table-keys)))
;;           job (select-keys row job-keys)]
;;       (update-node txn node-id row user-id)
;;       (when (seq job)
;;         (sql/update! db :job job ["id = ?" job-id])))))

;; (s/defn delete-job
;;   "Delete an job by id."
;;   [db job-id :- s/Int]
;;   (jdbc/with-db-transaction [txn db]
;;     (let [node-id (job-id->node-id db job-id)]
;;       (sql/delete! txn :job ["id = ?" job-id])
;;       (delete-node txn node-id))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ;; Workflow
;; (def ^:private default-workflow-params
;;   {:id-nil? true :id nil})

;; (s/defn ^:private select-workflow*
;;   [db m]
;;   (select-workflow db (merge default-workflow-params m)))

;; (s/defn select-workflow-by-id :- (s/maybe m/Workflow)
;;   "Select an workflow by id."
;;   [db id :- s/Int]
;;   (first (select-workflow* db {:id-nil? false :id id})))

;; (s/defn workflow-id->node-id :- s/Int
;;   [db workflow-id :- s/Int]
;;   (let [row (first (sql/query db ["select node_id as id from workflow where id = ?" workflow-id]))]
;;     (assert row)
;;     (:id row)))

;; (s/defn node-id->workflow-id :- s/Int
;;   [db node-id :- s/Int]
;;   (let [{:keys [node-type-id id]} (select-node-id->child-id db {:node-id node-id})]
;;     (assert (= node-type-id m/workflow-node-type-id))
;;     id))

;; (s/defn insert-workflow :- s/Int
;;   [db new-workflow :- m/WorkflowCreate user-id :- s/Int]
;;   (jdbc/with-db-transaction [txn db]
;;     (let [node-id (insert-node txn (assoc new-workflow :node-type-id m/workflow-node-type-id) user-id)]
;;       (:id (sql/insert! txn :workflow {:node-id node-id})))))

;; (s/defn update-workflow :- s/Int
;;   [db workflow-id :- s/Int row :- m/WorkflowAmend user-id :- s/Int]
;;   ;;no  props on workflow to update
;;   (jdbc/with-db-transaction [txn db]
;;     (let [node-id (workflow-id->node-id txn workflow-id)]
;;       (update-node txn node-id row user-id))))

;; (s/defn delete-workflow
;;   "Delete an workflow by id."
;;   [db workflow-id :- s/Int]
;;   (jdbc/with-db-transaction [txn db]
;;     (let [node-id (workflow-id->node-id db workflow-id)]
;;       (sql/delete! db :workflow ["id = ?" workflow-id])
;;       (delete-node txn node-id))))

;; ;; --- Job Schedule
;; (s/defn insert-job-schedule :- s/Int
;;   [db job-id :- s/Int schedule-id :- s/Int user-id :- s/Int]
;;   (jdbc/with-db-transaction [txn db]
;;     (let [node-id (job-id->node-id txn job-id)]
;;       (:id (sql/insert! txn :node-schedule {:node-id node-id :schedule-id schedule-id} user-id)))))

;; (s/defn delete-job-schedule
;;   [db job-schedule-id :- s/Int]
;;   (sql/delete! db :node-schedule ["id = ?" job-schedule-id]))


;; ;; --- Workflow Schedule
;; (s/defn insert-workflow-schedule :- s/Int
;;   [db workflow-id :- s/Int schedule-id :- s/Int user-id :- s/Int]
;;   (jdbc/with-db-transaction [txn db]
;;     (let [node-id (workflow-id->node-id txn workflow-id)]
;;       (log/infof "workflow %s tr to node %s" workflow-id node-id)
;;       (:id (sql/insert! txn :node-schedule {:node-id node-id :schedule-id schedule-id} user-id)))))

;; (s/defn delete-workflow-schedule
;;   [db workflow-schedule-id :- s/Int]
;;   (sql/delete! db :node-schedule ["id = ?" workflow-schedule-id]))


;; (def ^:private default-node-schedule-params
;;   {:id-nil? true :id nil
;;    :node-id-nil? true :node-id nil
;;    :schedule-id-nil? true :schedule-id nil})

;; (s/defn select-node-schedule*
;;   [db params rename-node-id]
;;   (map #(set/rename-keys % {:child-id rename-node-id})
;;        (select-node-schedule db (merge default-node-schedule-params params))))

;; (s/defn select-job-schedule-by-id
;;   [db id :- s/Int]
;;   (first (select-node-schedule* db {:id-nil? false :id id} :job-id)))

;; (s/defn select-job-schedule-by-schedule-id
;;   [db id :- s/Int]
;;   (select-node-schedule* db {:schedule-id-nil? false :schedule-id id} :job-id))

;; (s/defn select-job-schedules-by-job-id
;;   [db id :- s/Int]
;;   (select-node-schedule* db {:node-id-nil? false :node-id id} :job-id))

;; (s/defn select-job-schedule
;;   [db job-id :- s/Int schedule-id :- s/Int]
;;   (first (select-node-schedule* db {:node-id-nil? false :node-id job-id
;;                                     :schedule-id-nil? false :schedule-id schedule-id} :job-id)))

;; (s/defn select-workflow-schedule-by-id
;;   [db id :- s/Int]
;;   (first (select-node-schedule* db {:id-nil? false :id id} :workflow-id)))

;; (s/defn select-workflow-schedule-by-schedule-id
;;   [db id :- s/Int]
;;   (select-node-schedule* db {:schedule-id-nil? false :schedule-id id} :workflow-id))

;; (s/defn select-workflow-schedules-by-workflow-id
;;   [db id :- s/Int]
;;   (select-node-schedule* db {:node-id-nil? false :node-id id} :workflow-id))

;; (s/defn select-workflow-schedule
;;   [db workflow-id :- s/Int schedule-id :- s/Int]
;;   (first (select-node-schedule* db {:node-id-nil? false :node-id workflow-id
;;                                     :schedule-id-nil? false :schedule-id schedule-id} :workflow-id)))
