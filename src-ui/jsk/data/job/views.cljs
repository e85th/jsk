(ns jsk.data.job.views
  (:require [devcards.core :as d :refer-macros [defcard-rg]]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.util :as u]
            [e85th.ui.rf.inputs :as inputs]
            [jsk.data.job.events :as e]
            [jsk.data.job.subs :as subs]
            [jsk.data.job.props :as props]
            [jsk.routes :as routes]))

(defn prevent-default
  [e]
  (.preventDefault e))

(def indicate-dropzone (partial u/event-target-add-class "jsk-dnd-dropzone-hover"))
(def conceal-dropzone (partial u/event-target-rm-class "jsk-dnd-dropzone-hover"))

(defn schedule-drop
  [e]
  (prevent-default e)
  (rf/dispatch [e/schedule-dnd-drop]))

(defn alert-drop
  [e]
  (prevent-default e)
  (rf/dispatch [e/alert-dnd-drop]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Job Alert
(defsnippet alert-item* "templates/ui/data/job/edit.html" [:.jsk-job-alert-list [:.jsk-job-alert-item first-child]]
  [{:keys [:db/id :alert/name]}]
  {[:.jsk-job-alert-item] (k/set-attr :key id)
   [:.jsk-job-alert-name] (k/content name)
   [:.jsk-job-alert-delete] (k/listen :on-click #(rf/dispatch [e/dissoc-job-alert id]))})

(defsnippet alert-list* "templates/ui/data/job/edit.html" [:.jsk-job-alert-list]
  [alerts]
  {[:.jsk-job-alert-list] (k/set-attr :on-drop alert-drop
                                      :on-drag-enter indicate-dropzone
                                      :on-drag-leave conceal-dropzone
                                      :on-drag-over prevent-default)
   [:.jsk-job-alert-items] (k/content (map alert-item* alerts))})

(defn alert-list
  []
  (let [alerts (rf/subscribe [subs/current-alerts])]
    [alert-list* @alerts]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Job Schedule
(defsnippet schedule-item* "templates/ui/data/job/edit.html" [:.jsk-job-schedule-list [:.jsk-job-schedule-item first-child]]
  [{:keys [:db/id :schedule/name]}]
  {[:.jsk-job-schedule-item] (k/set-attr :key id)
   [:.jsk-job-schedule-name] (k/content name)
   [:.jsk-job-schedule-delete] (k/listen :on-click #(rf/dispatch [e/dissoc-job-schedule id]))})

(defsnippet schedule-list* "templates/ui/data/job/edit.html" [:.jsk-job-schedule-list]
  [schedules]
  {[:.jsk-job-schedule-list] (k/set-attr :on-drop schedule-drop
                                         :on-drag-enter indicate-dropzone
                                         :on-drag-leave conceal-dropzone
                                         :on-drag-over prevent-default)
   [:.jsk-job-schedule-items] (k/content (map schedule-item* schedules))})

(defn schedule-list
  []
  (let [schedules (rf/subscribe [subs/current-schedules])]
    [schedule-list* @schedules]))

(defsnippet job-view* "templates/ui/data/job/edit.html" [:.jsk-job-edit]
  [job-type-schema]
  {[:.job-name] (k/substitute [inputs/std-text subs/current-name e/current-name-changed])
   [:.job-desc] (k/substitute [inputs/std-text subs/current-desc e/current-desc-changed])
   [:.job-type] (k/substitute [inputs/label subs/current-type])
   [:.job-props] (k/content (props/schema->input-controls job-type-schema))
   [:.job-enabled] (k/substitute [inputs/checkbox subs/current-enabled? e/current-enabled?-changed])
   [:.job-save-btn] (k/substitute [inputs/button subs/busy? e/save-job "Save"])})


(defsnippet job-editor-layout* "templates/ui/data/job/edit.html" [:.jsk-job-edit-layout]
  [job-details-view]
  {[:#jsk-job-details-section] (k/content job-details-view)
   [:#jsk-job-schedules-section] (k/content [schedule-list])
   [:#jsk-job-alerts-section] (k/content [alert-list])})

(defn job-editor
  ([id]
   (rf/dispatch [e/fetch-job id])
   [job-editor])
  ([]
   (let [job-type-schema (rf/subscribe [subs/current-job-type-schema])]
     ;(log/infof "job-type-schema: %s" @job-type-schema)
     [job-editor-layout* [job-view* @job-type-schema]])))
