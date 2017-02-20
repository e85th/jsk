(ns jsk.data.job.views
  (:require [devcards.core :as d :refer-macros [defcard-rg]]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.rf.inputs :as inputs]
            [jsk.data.job.events :as e]
            [jsk.data.job.subs :as subs]
            [jsk.data.job.props :as props]
            [jsk.routes :as routes]))


(defsnippet job-item* "templates/ui/data/job/list.html" [:.jsk-job-list [:.jsk-job-item first-child]]
  [{:keys [db/id job/name]}]
  {[:.jsk-job-item] (k/do->
                       (k/set-attr :key id :href (routes/url-for :jsk.explorer/job :id id))
                       (k/content name))})

(defsnippet job-list* "templates/ui/data/job/list.html" [:.jsk-job-list]
  [jobs]
  {[:.jsk-job-list] (k/content (map job-item* jobs))})


(defn job-list
  []
  (rf/dispatch [e/fetch-job-list])
  (let [jobs (rf/subscribe [subs/job-list])]
    (fn []
      [job-list* @jobs])))

(defsnippet job-view* "templates/ui/data/job/edit.html" [:.jsk-job-edit]
  [job-type-schema]
  {[:.job-name] (k/substitute [inputs/std-text subs/current-name e/current-name-changed])
   [:.job-desc] (k/substitute [inputs/std-text subs/current-desc e/current-desc-changed])
   [:.job-type] (k/substitute [inputs/label subs/current-type])
   [:.job-props] (k/content (props/schema->input-controls job-type-schema))
   [:.job-enabled] (k/substitute [inputs/checkbox subs/current-enabled? e/current-enabled?-changed])
   [:.job-save-btn] (k/substitute [inputs/button subs/busy? e/save-job "Save"])})

(defn job-editor
  ([id]
   (rf/dispatch [e/fetch-job id])
   [job-editor])
  ([]
   (let [job-type-schema (rf/subscribe [subs/current-job-type-schema])]
     (log/infof "job-type-schema: %s" @job-type-schema)
     [job-view* @job-type-schema])))
