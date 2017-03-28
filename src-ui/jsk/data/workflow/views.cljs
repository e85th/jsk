(ns jsk.data.workflow.views
  (:require [devcards.core :as d :refer-macros [defcard-rg]]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.rf.inputs :as inputs]
            [e85th.ui.util :as u]
            [e85th.ui.rf.plumb :as plumb]
            [hipo.core :as hipo]
            [jsk.data.workflow.models :as m]
            [jsk.data.workflow.events :as e]
            [jsk.data.workflow.subs :as subs]
            [jsk.routes :as routes]))


(def indicate-dropzone (partial u/event-target-add-class "jsk-dnd-dropzone-hover"))
(def conceal-dropzone (partial u/event-target-rm-class "jsk-dnd-dropzone-hover"))

#_(defsnippet workflow-node* "templates/ui/data/workflow/designer.html" [:.jsk-workflow-node]
  [div-id node-name]
  {[:.jsk-workflow-node] (k/set-attr :id div-id)
   [:.jsk-workflow-node-name] (k/content node-name)})

(defn connection-click-listener
  [pb cn]
  (plumb/detach-connection pb cn))

(defn delete-workflow-node
  [pb div-id err-ep-id ok-ep-id]
  (plumb/rm-endpoint pb ok-ep-id)
  (plumb/rm-endpoint pb err-ep-id)
  (plumb/rm-inbound-connections pb div-id)
  (u/rm-element-by-id div-id))

(defn workflow-node*
  [pb div-id err-ep-id ok-ep-id node-name]
  [:div {:id div-id :class "jsk-workflow-node"}
   [:button {:type "button" :class "close" :on-click #(delete-workflow-node pb div-id err-ep-id ok-ep-id) } "x"]
   [:p]
   [:div.jsk-workflow-node-name node-name]
   [:p
    [:div.jsk-workflow-node-err-ep {:id err-ep-id} "Err"]
    [:div.jsk-workflow-node-ok-ep {:id ok-ep-id} "OK"]]])

;; FIXME: this should work in component-did-mount
(defn ensure-container
  [pb div-id]
  (if-let [c (plumb/container pb)]
    c
    (do
      (plumb/container pb div-id)
      (plumb/container pb))))

(defn node-fn
  [pb node]
  (log/infof "dropped on designer: %s" node)
  (let [container (ensure-container pb "am-wfd")
        id (str (gensym "wf-node-"))
        err-ep-id (str (gensym "wf-node-err-ep-"))
        ok-ep-id (str (gensym "wf-node-ok-ep-"))
        el (hipo/create (workflow-node* pb id err-ep-id ok-ep-id (:text node)))]
    (.appendChild container el)
    (plumb/draggable pb id)
    (plumb/make-source pb err-ep-id m/err-endpoint-opts)
    (plumb/make-source pb ok-ep-id m/ok-endpoint-opts)
    (plumb/make-target pb id m/target-opts)))

(defn designer-drop
  [pb e]
  (u/event-prevent-default e)
  (let [event {:client-x (.-clientX e)
               :client-y (.-clientY e)}]
    (rf/dispatch [e/designer-dnd-drop (fn [node] (node-fn pb node))])))

(defn schedule-drop
  [e]
  (u/event-stop-propogation e)
  (conceal-dropzone)
  (rf/dispatch [e/schedule-dnd-drop]))

(defn alert-drop
  [e]
  (u/event-stop-propogation e)
  (conceal-dropzone)
  (rf/dispatch [e/alert-dnd-drop]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workflow Alert
(defsnippet alert-item* "templates/ui/data/workflow/edit.html" [:.jsk-workflow-alert-list [:.jsk-workflow-alert-item first-child]]
  [{:keys [:db/id :alert/name]}]
  {[:.jsk-workflow-alert-item] (k/set-attr :key id)
   [:.jsk-workflow-alert-name] (k/content name)
   [:.jsk-workflow-alert-delete] (k/listen :on-click #(rf/dispatch [e/dissoc-workflow-alert id]))})

(defsnippet alert-list* "templates/ui/data/workflow/edit.html" [:.jsk-workflow-alert-list]
  [alerts]
  {[:.jsk-workflow-alert-list] (k/set-attr :on-drop alert-drop
                                           :on-drag-enter indicate-dropzone
                                           :on-drag-leave conceal-dropzone
                                           :on-drag-over u/event-prevent-default)
   [:.jsk-workflow-alert-items] (k/content (map alert-item* alerts))})

(defn alert-list
  []
  (let [alerts (rf/subscribe [subs/current-alerts])]
    [alert-list* @alerts]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workflow Schedule
(defsnippet schedule-item* "templates/ui/data/workflow/edit.html" [:.jsk-workflow-schedule-list [:.jsk-workflow-schedule-item first-child]]
  [{:keys [:db/id :schedule/name]}]
  {[:.jsk-workflow-schedule-item] (k/set-attr :key id)
   [:.jsk-workflow-schedule-name] (k/content name)
   [:.jsk-workflow-schedule-delete] (k/listen :on-click #(rf/dispatch [e/dissoc-workflow-schedule id]))})

(defsnippet schedule-list* "templates/ui/data/workflow/edit.html" [:.jsk-workflow-schedule-list]
  [schedules]
  {[:.jsk-workflow-schedule-list] (k/set-attr :on-drop schedule-drop
                                              :on-drag-enter indicate-dropzone
                                              :on-drag-leave conceal-dropzone
                                              :on-drag-over u/event-prevent-default)
   [:.jsk-workflow-schedule-items] (k/content (map schedule-item* schedules))})

(defn schedule-list
  []
  (let [schedules (rf/subscribe [subs/current-schedules])]
    [schedule-list* @schedules]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workflow Main Area
(defsnippet workflow-view* "templates/ui/data/workflow/edit.html" [:.jsk-workflow-edit]
  []
  {[:.workflow-name] (k/substitute [inputs/std-text subs/current-name e/current-name-changed])
   [:.workflow-desc] (k/substitute [inputs/std-text subs/current-desc e/current-desc-changed])
   [:.workflow-enabled] (k/substitute [inputs/checkbox subs/current-enabled? e/current-enabled?-changed])
   [:.workflow-save-btn] (k/substitute [inputs/button subs/busy? e/save-workflow "Save"])})

(defn workflow-designer*
  []
  (let [pb (plumb/new-instance m/jsk-plumb-defaults)]
    [plumb/control
     {:on-drop (partial designer-drop pb)
      :on-drag-enter u/event-prevent-default
      :on-drag-over u/event-prevent-default
      :class "jsk-workflow-designer"
      :id "am-wfd"}
     {}
     (fn [pb]
       (plumb/register-connection-click-handler pb (partial connection-click-listener pb))
       (log/infof "designer mounted"))
     (fn [pb]
       (log/infof "designer unmounted"))]))

(defsnippet workflow-editor-layout* "templates/ui/data/workflow/edit.html" [:.jsk-workflow-edit-layout]
  []
  {[:#jsk-workflow-designer-section] (k/content [workflow-designer*])
   [:#jsk-workflow-details-section] (k/content [workflow-view*])
   [:#jsk-workflow-schedules-section] (k/content [schedule-list])
   [:#jsk-workflow-alerts-section] (k/content [alert-list])})


(defn workflow-editor
  ([id]
   (rf/dispatch [e/fetch-workflow id])
   [workflow-editor])
  ([]
   [workflow-editor-layout*]))
