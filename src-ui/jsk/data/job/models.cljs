(ns jsk.data.job.models)

(def current [::current])

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
(def job-list [::job-list])


;; map of keyword to vector of maps
(def job-types [::job-types])

(def new-job
  {:job/behavior :standard
   :job/context-key ""
   :job/desc ""
   :job/enabled? true
   :job/max-concurrent 1
   :job/max-retries 0
   :job/name ""
   :job/props {}
   :job/schedules []
   :job/tags []
   :job/timeout-ms 0
   :job/type :shell})
