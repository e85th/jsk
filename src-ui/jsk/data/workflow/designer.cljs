(ns jsk.data.workflow.designer
  (:require [devcards.core :as d :refer-macros [defcard-rg]]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.rf.inputs :as inputs]
            [e85th.ui.util :as u]
            [e85th.ui.rf.plumb :as plumb]
            [hipo.core :as hipo]
            [jsk.data.workflow.models :as m]
            [clojure.string :as str]))


(defn workflow-node*
  [dom-id err-source-id ok-source-id node-name {:keys [left top]} node-removed-event]
  [:div {:id dom-id :class "jsk-workflow-node" :style (str "left: " left "px; top: " top "px;")}
   [:button {:type "button" :class "close" :on-click #(rf/dispatch [node-removed-event dom-id])} "x"]
   [:p]
   [:div.jsk-workflow-node-name node-name]
   [:p
    [:div.jsk-workflow-node-err-source {:id err-source-id}]
    [:div.jsk-workflow-node-ok-source {:id ok-source-id}]]])

(defn compute-placement-coords
  [element {:keys [client-x client-y]}]
  (let [{:keys [top left]} (u/bounding-rect element)]
    {:left (- client-x left)
     :top (- client-y top)}))

(defn create-source-id
  [wf-node-id kind]
  (str wf-node-id "|" kind))

(defn source-id->components
  [source-id]
  (let [[wf-node-id kind] (str/split source-id "|")]
    {:node-id wf-node-id
     :kind (keyword kind)}))

(defn add-workflow-node
  "Adds a new workflow node to the designer and returns the dom-id"
  [pb node drop-cords node-removed-event]
  (let [container (plumb/container pb)
        dom-id (str (gensym "wf-node-"))
        err-source-id (create-source-id dom-id "err")
        ok-source-id (create-source-id dom-id "ok")
        placement-coords (compute-placement-coords container drop-cords)
        el (hipo/create (workflow-node* dom-id err-source-id ok-source-id (:text node) placement-coords node-removed-event))]
    (.appendChild container el)
    (plumb/draggable pb dom-id)
    (plumb/make-source pb err-source-id m/err-source-opts)
    (plumb/make-source pb ok-source-id m/ok-source-opts)
    (plumb/make-target pb dom-id m/target-opts)
    dom-id))
