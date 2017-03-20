(ns jsk.data.schedule.views
  (:require [devcards.core :as d :refer-macros [defcard-rg]]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.rf.inputs :as inputs]
            [jsk.data.schedule.events :as e]
            [jsk.data.schedule.subs :as subs]
            [jsk.routes :as routes]))

(defsnippet schedule-view* "templates/ui/data/schedule/edit.html" [:.jsk-schedule-edit]
  []
  {[:.schedule-name] (k/substitute [inputs/std-text subs/current-name e/current-name-changed])
   [:.schedule-desc] (k/substitute [inputs/std-text subs/current-desc e/current-desc-changed])
   [:.schedule-cron-expr] (k/substitute [inputs/std-text subs/current-cron-expr e/current-cron-expr-changed])
   [:.schedule-save-btn] (k/substitute [inputs/button subs/busy? e/save-schedule "Save"])})

(defn schedule-editor
  ([id]
   (rf/dispatch [e/fetch-schedule id])
   [schedule-editor])
  ([]
   [schedule-view*]))
