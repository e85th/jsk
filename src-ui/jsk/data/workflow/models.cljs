(ns jsk.data.workflow.models)

(def current [::current])

(def current-desc [::current :workflow/desc])
(def current-enabled? [::current :workflow/enabled?])
(def current-id [::current :db/id])
(def current-name [::current :workflow/name])
(def current-schedules [::current :workflow/schedules])
(def current-tags [::current :workflow/tags])

(def busy? [::busy?])
