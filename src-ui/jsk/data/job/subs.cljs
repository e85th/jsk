(ns jsk.data.job.subs
  (:require [re-frame.core :as rf]
            [e85th.ui.rf.macros :refer-macros [defsub defsub-db]]
            [taoensso.timbre :as log]
            [jsk.data.job.models :as m]))

(defsub current-alerts m/current-alerts)
(defsub current-behavior m/current-behavior)
(defsub current-context-key m/current-context-key)
(defsub current-desc m/current-desc)
(defsub current-enabled? m/current-enabled?)
(defsub current-max-concurrent m/current-max-concurrent)
(defsub current-max-retries m/current-max-retries)
(defsub current-name m/current-name)
(defsub current-props m/current-props)
(defsub current-schedules m/current-schedules)
(defsub current-tags m/current-tags)
(defsub current-timeout-ms m/current-timeout-ms)
(defsub current-type m/current-type)

(defsub busy? m/busy?)

(defsub job-types m/job-types)

(defsub-db job-type-names
  [db _]
  (let [job-types (rf/subscribe job-types)]
    (->> @job-types
         keys
         (map str)
         sort)))

;; FIXME: subs seem to run anytime the db changes when this sub is in play
;; no reaction on return on any subs to prevent the signal graph from executing
(defsub-db current-job-type-schema
  [db _]
  (let [current-type (rf/subscribe [current-type])
        job-types (rf/subscribe [job-types])]
    #_(log/infof "job-types: %s" @job-types)
    #_(log/infof "current-type: %s" @current-type)
    (get @job-types @current-type [])))

;; -- Handles dynamic subscription inside of props map
(defsub-db current-props-field-value
  [db [_ props-key]]
  (get-in db (conj m/current-props props-key)))
