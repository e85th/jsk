(ns jsk.data.workflow.views
  (:require [devcards.core :as d :refer-macros [defcard-rg]]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.rf.inputs :as inputs]
            [jsk.data.workflow.events :as e]
            [jsk.data.workflow.subs :as subs]
            [jsk.routes :as routes]))

(defsnippet workflow-view* "templates/ui/data/workflow/edit.html" [:.jsk-workflow-edit]
  []
  {[:.workflow-name] (k/substitute [inputs/std-text subs/current-name e/current-name-changed])
   [:.workflow-desc] (k/substitute [inputs/std-text subs/current-desc e/current-desc-changed])
   [:.workflow-enabled] (k/substitute [inputs/checkbox subs/current-enabled? e/current-enabled?-changed])
   [:.workflow-save-btn] (k/substitute [inputs/button subs/busy? e/save-workflow "Save"])})

(defn workflow-editor
  ([id]
   (rf/dispatch [e/fetch-workflow id])
   [workflow-editor])
  ([]
   [workflow-view*]))
