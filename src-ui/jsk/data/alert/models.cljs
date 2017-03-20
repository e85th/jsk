(ns jsk.data.alert.models)

(def current [::current])
(def current-id [::current :db/id])
(def current-name [::current :alert/name])
(def current-desc [::current :alert/desc])
(def current-channels [::current :alert/channels-info])

(def busy? [::busy?])

(def selected-channel [::selected-channel])

(def channel-suggestions [::channel-suggestions])
