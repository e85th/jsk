(ns jsk.data.explorer.views
  (:require [devcards.core :as d :refer-macros [defcard-rg]]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.rf.inputs :as inputs]
            [jsk.data.explorer.subs :as subs]
            [jsk.data.agent.views :as agent-views]
            [jsk.data.schedule.views :as schedule-views]
            [jsk.data.job.views :as job-views]
            [jsk.data.job.events :as job-events]
            [jsk.routes :as routes]))

;; FIXME: this should be done with a event
(defn update-history
  [handler]
  (let [url (routes/url-for handler)
        res (js/window.history.pushState nil nil url)]
    (log/infof "update-history url: %s res: %s" url res)))


(defsnippet explorer* "templates/ui/data/explorer/explorer.html" [:.jsk-explorer]
  [main-content]
  {;[:#jsk-explorer-executables-tab] (k/listen :on-click #(update-history :jsk.explorer/job-list))
   [:#jsk-explorer-executables] (k/content [job-views/job-list])
   [:#jsk-explorer-schedules] (k/content [schedule-views/schedule-list-with-actions])
   [:#jsk-explorer-agents] (k/content [agent-views/agent-list])
   [:.jsk-explorer-main] (k/content main-content)})

(defn route->id
  [route]
  (get-in route [:route-params :id]))

(defmulti explorer-panels :handler)


;; -- Agents
(defmethod explorer-panels :jsk.explorer/agent-list
  [route]
  (update-history route)
  [:h4 "Agents"])
(defmethod explorer-panels :jsk.explorer/job-list
  [route]
  (update-history route)
  [:h4 "Jobs"])
(defmethod explorer-panels :jsk.explorer/schedule-list
  [route]
  (update-history route)
  [:h4 "Schedules"])

(defmethod explorer-panels :jsk.explorer/agent [route]
  [agent-views/agent-editor (route->id route)])

(defmethod explorer-panels :jsk.explorer/job [route]
  [job-views/job-editor (route->id route)])

(defmethod explorer-panels :jsk.explorer/schedule [route]
  [schedule-views/schedule-editor (route->id route)])

(defmethod explorer-panels :default
  [route]
  [:h3 (str "No explorer-panel handler for: " route)])

(defn explorer
  []
  (rf/dispatch [job-events/fetch-job-types])
  (let [matched-panel (rf/subscribe [subs/explorer-view])]
    (fn []
      [explorer* (explorer-panels @matched-panel)])))
