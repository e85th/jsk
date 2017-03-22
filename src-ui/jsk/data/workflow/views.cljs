(ns jsk.data.workflow.views
  (:require [devcards.core :as d :refer-macros [defcard-rg]]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.rf.inputs :as inputs]
            [jsk.data.workflow.events :as e]
            [jsk.data.workflow.subs :as subs]
            [jsk.routes :as routes]))


(defn prevent-default
  [e]
  (.preventDefault e))

(defn designer-drop
  [e]
  (prevent-default e)
  (let [event {:client-x (.-clientX e)
               :client-y (.-clientY e)}]
    (rf/dispatch [e/designer-dnd-drop event])))

(defn schedule-drop
  [e]
  (prevent-default e)
  (rf/dispatch [e/schedule-dnd-drop]))

(defn alert-drop
  [e]
  (prevent-default e)
  (rf/dispatch [e/alert-dnd-drop]))


(defsnippet workflow-view* "templates/ui/data/workflow/edit.html" [:.jsk-workflow-edit]
  []
  {[:.workflow-name] (k/substitute [inputs/std-text subs/current-name e/current-name-changed])
   [:.workflow-desc] (k/substitute [inputs/std-text subs/current-desc e/current-desc-changed])
   [:.workflow-enabled] (k/substitute [inputs/checkbox subs/current-enabled? e/current-enabled?-changed])
   [:.workflow-save-btn] (k/substitute [inputs/button subs/busy? e/save-workflow "Save"])})

(defsnippet workflow-designer* "templates/ui/data/workflow/edit.html" [:.jsk-workflow-designer]
  []
  {[:.jsk-workflow-designer] (k/set-attr :on-drop designer-drop
                                         :on-drag-enter prevent-default
                                         :on-drag-over prevent-default)})

(defsnippet workflow-editor-layout* "templates/ui/data/workflow/edit.html" [:.jsk-workflow-edit-layout]
  []
  {[:#jsk-workflow-designer-section] (k/content [workflow-designer*])
   [:#jsk-workflow-details-section] (k/content [workflow-view*])
   [:#jsk-workflow-schedules-section] (k/content [:h3 "Schedules here"])
   [:#jsk-workflow-alerts-section] (k/content [:h3 "Alerts here"])})


(defn workflow-editor
  ([id]
   (rf/dispatch [e/fetch-workflow id])
   [workflow-editor])
  ([]
   [workflow-editor-layout*]))
