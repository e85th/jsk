(ns jsk.data.agent.views
  (:require [devcards.core :as d :refer-macros [defcard-rg]]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.rf.inputs :as inputs]
            [jsk.data.agent.events :as e]
            [jsk.data.agent.subs :as subs]
            [jsk.routes :as routes]))

(defsnippet agent-actions* "templates/ui/data/agent/list.html" [:.jsk-agent-action-bar]
  []
  {[:.jsk-new-agent-action] (k/listen :on-click #(rf/dispatch [e/new-agent]))})

(defsnippet agent-item* "templates/ui/data/agent/list.html" [:.jsk-agent-list [:.jsk-agent-item first-child]]
  [{:keys [db/id agent/name]}]
  {[:.jsk-agent-item] (k/do->
                       (k/set-attr :key id :href (routes/url-for :jsk.explorer/agent :id id))
                       (k/content name))})

(defsnippet agent-list* "templates/ui/data/agent/list.html" [:.jsk-agent-list]
  [agents]
  {[:.jsk-agent-list] (k/content (map agent-item* agents))})


(defn agent-list
  []
  (rf/dispatch [e/fetch-agent-list])
  (let [agents (rf/subscribe [subs/agent-list])]
    (fn []
      [agent-list* @agents])))

(defn agent-list-with-actions
  []
  [:div
   [agent-actions*]
   [agent-list]])

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
