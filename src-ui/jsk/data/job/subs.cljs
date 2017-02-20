(ns jsk.data.job.subs
  (:require [re-frame.core :as rf]
            [e85th.ui.rf.sweet :refer-macros [def-sub-db def-sub]]
            [taoensso.timbre :as log]
            [jsk.data.job.models :as m]))

(def-sub-db current-behavior m/current-behavior)
(def-sub-db current-context-key m/current-context-key)
(def-sub-db current-desc m/current-desc)
(def-sub-db current-enabled? m/current-enabled?)
(def-sub-db current-max-concurrent m/current-max-concurrent)
(def-sub-db current-max-retries m/current-max-retries)
(def-sub-db current-name m/current-name)
(def-sub-db current-props m/current-props)
(def-sub-db current-schedules m/current-schedules)
(def-sub-db current-tags m/current-tags)
(def-sub-db current-timeout-ms m/current-timeout-ms)
(def-sub-db current-type m/current-type)

(def-sub-db busy? m/busy?)

(def-sub-db job-list m/job-list)

(def-sub-db job-types m/job-types)

(def-sub job-type-names
  [db _]
  (let [job-types (rf/subscribe job-types)]
    (->> @job-types
         keys
         (map str)
         sort)))

(def-sub current-job-type-schema
  [db _]
  (let [current-type (rf/subscribe [current-type])
        job-types (rf/subscribe [job-types])]
    (log/infof "job-types: %s" @job-types)
    (log/infof "current-type: %s" @current-type)
    (get @job-types @current-type [])))

;; -- Handles dynamic subscription inside of props map
(def-sub current-props-field-value
  [db [_ props-key]]
  (get-in db (conj m/current-props props-key)))
