(ns jsk.data.workflow.models)

(def current [::current])

(def current-alerts [::current :workflow/alerts])
(def current-desc [::current :workflow/desc])
(def current-enabled? [::current :workflow/enabled?])
(def current-id [::current :db/id])
(def current-name [::current :workflow/name])
(def current-schedules [::current :workflow/schedules])
(def current-tags [::current :workflow/tags])

(def busy? [::busy?])

(def jsk-plumb-defaults
  {:Anchor "Continuous"
   :Connector [:StateMachine {:curviness 20}]
   :ConnectionOverlays [[:Arrow {:location 1 :id :arrow :length 14 :width 9 :foldback 0.7}]]
   :Endpoint           [:Dot {:radius 2}]
   :Endpoints          [[:Dot {:radius 2}] [:Dot {:radius 4}]]
   :HoverPaintStyle    {:strokeStyle "#1e8151" :lineWidth 2}})

;;-----------------------------------------------------------------------
;; Definitions governing success endpoints
;; Lines are green and solid indicating succcess job link.
;;-----------------------------------------------------------------------
(def ok-endpoint-opts
  {:cssClass "jsk-ok-endpoint"
   :connectorStyle {:stroke :green :strokeWidth 2 :outlineStroke "transparent" :outlineWidth 4}})

;;-----------------------------------------------------------------------
;; Definitions governing failure endpoints
;; Lines are red and broken indicating failed job link.
;;-----------------------------------------------------------------------
(def err-endpoint-opts
  {:cssClass "jsk-err-endpoint"
   :connectorStyle {:stroke :red :strokeWidth 2 :dashstyle "2 2" :outlineStroke "transparent" :outlineWidth 4}})

(def target-opts
  {:dropOptions {:hoverClass :dragHover}
   :allowLoopback false
   :anchor :Continuous})
