(ns jsk.data.workflow.designer
  (:require [devcards.core :as d :refer-macros [defcard-rg]]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.rf.inputs :as inputs]
            [e85th.ui.util :as u]
            [e85th.ui.dom :as dom]
            [e85th.ui.rf.plumb :as plumb]
            [hipo.core :as hipo]
            [jsk.data.workflow.models :as m]
            [jsk.data.workflow.graph :as g]
            [clojure.string :as str]))


(defn workflow-node*
  [dom-id err-source-id ok-source-id node-name style node-removed-event]
  [:div {:id dom-id :class "jsk-workflow-node" :style style}
   [:button {:type "button" :class "close" :on-click #(rf/dispatch [node-removed-event dom-id])} "x"]
   [:p]
   [:div.jsk-workflow-node-name node-name]
   [:p
    [:div.jsk-workflow-node-err-source {:id err-source-id}]
    [:div.jsk-workflow-node-ok-source {:id ok-source-id}]]])

(defn compute-placement-coords
  [element {:keys [client-x client-y] :as drop-coords}]
  (let [{:keys [top left]} (dom/bounding-rect element)]
    (if drop-coords
      {:left (- client-x left)
       :top (- client-y top)}
      {:left 0
       :top 0})))

(defn coords->style
  [{:keys [top left]}]
  (str "left: " left "px; top: " top "px;"))

(defn cons-source-id
  [wf-node-id kind]
  (str wf-node-id "|" kind))

(defn decons-source-id
  [source-id]
  (let [[wf-node-id kind] (str/split source-id "|")]
    {:node-id wf-node-id
     :kind (keyword kind)}))

(defn add-workflow-node
  "Adds a new workflow node to the designer and returns the dom-id"
  [pb node-dom-id node-name node-type style node-removed-event]
  (let [container (plumb/container pb)
        err-source-id (cons-source-id node-dom-id "successors-err")
        ok-source-id (cons-source-id node-dom-id "successors")
        el (hipo/create (workflow-node* node-dom-id err-source-id ok-source-id node-name style node-removed-event))]
    (.appendChild container el)
    (plumb/draggable pb node-dom-id)
    (plumb/make-source pb err-source-id m/err-source-opts)
    (plumb/make-source pb ok-source-id m/ok-source-opts)
    (plumb/make-target pb node-dom-id m/target-opts)
    {:ok-source-id ok-source-id
     :err-source-id err-source-id}))

(defn add-dnd-workflow-node
  "Adds a new workflow node to the designer. Calculates the offset based on drop-cords to
   place the element where the mouse was."
  [pb node-dom-id node-name node-type drop-cords node-removed-event]
  (let [container (plumb/container pb)
        style (coords->style (compute-placement-coords container drop-cords))]
    (add-workflow-node pb node-dom-id node-name node-type style node-removed-event)))


(defn- add-all-graph-nodes
  "Adds all nodes and returns a map of dom-id to a map of ok-source-id, err-source-id"
  [pb graph node-removed-event]
  (reduce (fn [m {:keys [node/id node/name node/type node/style]}]
            (assoc m id (add-workflow-node pb id name type style node-removed-event)))
          {}
          (g/nodes graph)))

(defn populate*
  "Adds all graph nodes then connects successors and successors-err"
  [pb graph node-removed-event]
  (let [nodes (g/nodes graph)
        dom-infos (add-all-graph-nodes pb graph node-removed-event)]
    (doseq [{:keys [node/id] :as node} nodes
            :let [{:keys [ok-source-id err-source-id]} (dom-infos id)]]
      (doseq [target-wf-node-id (:node/successors node)]
        (plumb/connect pb ok-source-id target-wf-node-id))
      (doseq [target-wf-node-id (:node/successors-err node)]
        (plumb/connect pb err-source-id target-wf-node-id)))))

(defn populate
  [pb graph node-removed-event]
  (plumb/empty pb)
  (plumb/batch pb #(populate* pb graph node-removed-event)))
