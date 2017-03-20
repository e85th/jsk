(ns jsk.data.agent.views
  (:require [devcards.core :as d :refer-macros [defcard-rg]]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.rf.inputs :as inputs]
            [jsk.data.agent.events :as e]
            [jsk.data.agent.subs :as subs]
            [jsk.routes :as routes]))

(defsnippet agent-view* "templates/ui/data/agent/edit.html" [:.jsk-agent-edit]
  []
  {[:.agent-name] (k/substitute [inputs/std-text subs/current-name e/name-changed])
   [:.agent-save-btn] (k/substitute [inputs/button subs/busy? e/save-agent "Save"])})

(defn agent-editor
  ([id]
   (rf/dispatch [e/fetch-agent id])
   [agent-editor])
  ([]
   [agent-view*]))
