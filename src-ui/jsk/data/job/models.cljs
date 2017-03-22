(ns jsk.data.job.models)

(def current [::current])

(def current-alerts [::current :job/alerts])
(def current-id [::current :db/id])
(def current-behavior [::current :job/behavior])
(def current-context-key [::current :job/context-key])
(def current-desc [::current :job/desc])
(def current-enabled? [::current :job/enabled?])
(def current-max-concurrent [::current :job/max-concurrent])
(def current-max-retries [::current :job/max-retries])
(def current-name [::current :job/name])
(def current-props [::current :job/props])
(def current-schedules [::current :job/schedules])
(def current-tags [::current :job/tags])
(def current-timeout-ms [::current :job/timeout-ms])
(def current-type [::current :job/type])

(def busy? [::busy?])

;; map of keyword to vector of maps
(def job-types [::job-types])

(defn prep-for-save
  "Schedules, tags and alerts are saved independently."
  [job]
  (dissoc job :job/schedules :job/tags :job/alerts))
