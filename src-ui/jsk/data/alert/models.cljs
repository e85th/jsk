(ns jsk.data.alert.models)

(def current [::current])
(def current-id [::current :db/id])
(def current-name [::current :alert/name])
(def current-desc [::current :alert/desc])
(def current-channels [::current :alert/channels-info])
(def alert-list [::alert-list])
(def busy? [::busy?])

(def new-alert
  {:alert/name ""
   :alert/desc ""
   :alert/channels-info []})

(def selected-channel [::selected-channel])

(def channel-suggestions [::channel-suggestions])
