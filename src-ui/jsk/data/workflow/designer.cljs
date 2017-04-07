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
  [dom-id err-source-id ok-source-id node-name {:keys [left top]} node-removed-event]
  [:div {:id dom-id :class "jsk-workflow-node" :style (str "left: " left "px; top: " top "px;")}
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
  [pb node-name node-type drop-cords node-removed-event]
  (let [container (plumb/container pb)
        dom-id (str (gensym "wf-node-"))
        err-source-id (cons-source-id dom-id "successors-err")
        ok-source-id (cons-source-id dom-id "successors")
        placement-coords (compute-placement-coords container drop-cords)
        el (hipo/create (workflow-node* dom-id err-source-id ok-source-id node-name placement-coords node-removed-event))]
    (log/infof "el: %s" el)
    (.appendChild container el)
    (plumb/draggable pb dom-id)
    (plumb/make-source pb err-source-id m/err-source-opts)
    (plumb/make-source pb ok-source-id m/ok-source-opts)
    (plumb/make-target pb dom-id m/target-opts)
    dom-id))


#_(defn populate
  [pb nodes node-removed-event]
  ;; node has keys :workflow.node/successors :workflow.node/successors-err
  ;; if wf node then :workflow/name and :workflow/id
  ;; if job node then :job/name and :job/id
  (let [dom-infos (reduce (fn [m {:keys [db/id] :as n}]
                            (let [node-name (or (:job/name n) (:workflow/name n))]
                              (assoc m id (add-workflow-node pb node-name nil node-removed-event))))
                          {}
                          nodes)]
    (log/infof "dom-infos: %s" dom-infos)
    ;; make-connections
    (mapv (fn [node]
            (let [{:keys [workflow.node/successors workflow.node/successors-err]} node
                  {:keys [ok-source-id err-source-id]} (dom-infos (:db/id node))]
              (doseq [successor successors
                      :let [{:keys [dom-id]} (dom-infos successor)]]
                (plumb/connect pb ok-source-id dom-id))
              (doseq [successor successors-err
                      :let [{:keys [dom-id]} (dom-infos successor)]]
                (plumb/connect pb err-source-id dom-id))))
          nodes)))
