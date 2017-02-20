(ns jsk.data.job.events
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [jsk.data.job.models :as m]
            [re-frame.core :as rf]
            [jsk.net.api :as api]
            [e85th.ui.util :as u]
            [e85th.ui.rf.sweet :refer-macros [def-event-db def-event-fx def-db-change]]
            [clojure.string :as str]))

(def-db-change current-name-changed m/current-name)
(def-db-change current-desc-changed m/current-desc)
(def-db-change current-enabled?-changed m/current-enabled?)

(def-event-fx rpc-err
  [{:keys [db] :as cofx} event-v]
  (log/warnf "rpc err: %s" event-v)
  (let [[db msg] (condp = (second event-v)
                   :rpc/fetch-job-err [db "Error fetching job."]
                   :rpc/fetch-job-types-err [db "Error fetching job types."]
                   :rpc/save-job-err [db "Error saving job."]
                   :rpc/delete-job-err [db "Error deleting job."]
                   :rpc/fetch-job-list-err [db "Error fetching job list."]
                   :else [db "Encountered unhandled RPC error."])]
    {:db (assoc-in db m/busy? false)
     :notify [:alert {:message msg}]}))

(def-db-change fetch-job-list-ok m/job-list)

(def-event-fx fetch-job-list
  [_ _]
  {:http-xhrio (api/fetch-job-list fetch-job-list-ok [rpc-err :rpc/fetch-job-list-err])})

(def-db-change fetch-job-ok m/current)

(def-event-fx fetch-job
  [_ [_ job-id]]
  {:http-xhrio (api/fetch-job job-id fetch-job-ok [rpc-err :rpc/fetch-job-err])})

(def-event-db save-job-ok
  [db [_ job]]
  (-> db
      (assoc-in m/current job)
      (assoc-in m/busy? false)))

(def-event-fx save-job
  [{:keys [db]} _]
  (let [job (get-in db m/current)]
    {:db (assoc-in db m/busy? true)
     :http-xhrio (api/save-job job save-job-ok [rpc-err :rpc/save-job-err])}))


(def-event-db current-props-field-value-changed
  [db [_ field-key field-value]]
  (assoc-in db (conj m/current-props field-key) field-value))

(def-db-change fetch-job-types-ok m/job-types)

(def-event-fx fetch-job-types
  [_ _]
  {:http-xhrio (api/fetch-job-types fetch-job-types-ok [rpc-err :rpc/fetch-job-types-err])})
