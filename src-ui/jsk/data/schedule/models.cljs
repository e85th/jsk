(ns jsk.data.schedule.models)

(def current [::current])

(def current-id [::current :db/id])
(def current-name [::current :schedule/name])
(def current-desc [::current :schedule/desc])
(def current-cron-expr [::current :schedule/cron-expr])

(def busy? [::busy?])
