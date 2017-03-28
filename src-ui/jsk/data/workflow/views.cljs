(ns jsk.data.workflow.views
  (:require [devcards.core :as d :refer-macros [defcard-rg]]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.rf.inputs :as inputs]
            [e85th.ui.util :as u]
            [e85th.ui.rf.plumb :as plumb]
            [jsk.data.workflow.models :as m]
            [jsk.data.workflow.events :as e]
            [jsk.data.workflow.subs :as subs]
            [jsk.routes :as routes]))



(def indicate-dropzone (partial u/event-target-add-class "jsk-dnd-dropzone-hover"))
(def conceal-dropzone (partial u/event-target-rm-class "jsk-dnd-dropzone-hover"))

(defn designer-drop
  [e]
  (u/event-prevent-default e)
  ;; e is a react event proxy, so have to access the props here otherwise
  ;; won't be available later
  (let [coords {:client-x (.-clientX e)
                :client-y (.-clientY e)}]
    (rf/dispatch [e/designer-dnd-drop coords])))

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
  [plumb/control
   {:on-drop designer-drop
    :on-drag-enter u/event-prevent-default
    :on-drag-over u/event-prevent-default
    :class "jsk-workflow-designer"
    :id m/designer-container-id}
   m/jsk-plumb-defaults
   (fn [pb]
     (plumb/container pb m/designer-container-id)
     (plumb/register-connection-click-handler pb #(rf/dispatch [e/detach-connection %]))
     (plumb/register-connection-created-handler pb #(rf/dispatch [e/connection-created %]))
     (rf/dispatch [e/set-plumb-instance pb])
     (log/infof "designer mounted"))
   (fn [pb]
     (rf/dispatch [e/set-plumb-instance nil]))])

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
