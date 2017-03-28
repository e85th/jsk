(ns jsk.data.workflow.models
  (:require [schema.core :as s]))

(def current [::current])

(def current-alerts [::current :workflow/alerts])
(def current-desc [::current :workflow/desc])
(def current-enabled? [::current :workflow/enabled?])
(def current-id [::current :db/id])
(def current-name [::current :workflow/name])
(def current-schedules [::current :workflow/schedules])
(def current-tags [::current :workflow/tags])

(def busy? [::busy?])

(def graph [::graph])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph stuff should be in a separate ns and graph should probably be a
;; record with a protocol
(s/defschema GraphNode
  {:id   s/Int ;; db/id
   ;; ok and err are the outbound edges for success
   :ok  #{s/Int}
   :err #{s/Int}})

(s/defschema Graph
  ;; the string key is the dom-id for either the job or workflow
  {s/Str GraphNode})

(defn graph-node
  "Creates a node. The id is the db/id for either a job or workflow."
  [id]
  {:id id
   :ok #{}
   :err #{}})

(defn graph-dom-id->db-id
  [graph dom-id]
  (get-in graph [dom-id :id]))

(defn- update-graph-edge
  [f graph source-dom-id target-id kind]
  (update-in graph [source-dom-id kind] f target-id))

(def graph-rm-edge (partial update-graph-edge disj))
(def graph-add-edge (partial update-graph-edge conj))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; JS Plumb related items
(def plumb-instance [::plumb-instance])

(def designer-container-id "jsk-workflow-design-container")

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
(def ok-source-opts
  {:cssClass "jsk-ok-source"
   :connectorStyle {:stroke :green :strokeWidth 2 :outlineStroke "transparent" :outlineWidth 4}})

;;-----------------------------------------------------------------------
;; Definitions governing failure endpoints
;; Lines are red and broken indicating failed job link.
;;-----------------------------------------------------------------------
(def err-source-opts
  {:cssClass "jsk-err-source"
   :connectorStyle {:stroke :red :strokeWidth 2 :dashstyle "2 2" :outlineStroke "transparent" :outlineWidth 4}})

(def target-opts
  {:dropOptions {:hoverClass :dragHover}
   :allowLoopback false
   :anchor :Continuous})
