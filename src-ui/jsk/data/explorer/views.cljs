(ns jsk.data.explorer.views
  (:require [devcards.core :as d :refer-macros [defcard-rg]]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.rf.inputs :as inputs]
            [e85th.ui.rf.tree :as tree]
            [jsk.data.explorer.models :as m]
            [jsk.data.explorer.subs :as subs]
            [jsk.data.explorer.events :as e]
            [jsk.data.explorer.menus :as menus]
            [jsk.data.explorer.dnd :as dnd]
            [jsk.data.agent.views :as agent-views]
            [jsk.data.alert.views :as alert-views]
            [jsk.data.schedule.views :as schedule-views]
            [jsk.data.job.views :as job-views]
            [jsk.data.job.events :as job-events]
            [jsk.routes :as routes]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tree Initialization
(defn fetch-children
  [node cb]
  (rf/dispatch [e/fetch-children (tree/normalize-node node) cb]))

(def tree-init-data
  ;; check_callback must be set to true otherwise operations like create, rename, are prevented
  {:core {:data fetch-children :check_callback true}
   :contextmenu {:items menus/context-menu-items}
   :dnd {:is_draggable dnd/draggable? :use_html5 true}
   :types {:directory {:icon "glyphicon glyphicon-folder-open"}
           :job {:icon "glyphicon glyphicon-hdd"}
           :workflow {:icon "glyphicon glyphicon-tasks"}
           :schedule {:icon "glyphicon glyphicon-time"}
           :alert {:icon "glyphicon glyphicon-bell"}
           :agent {:icon "glyphicon glyphicon-cloud"}
           :section {:icon "glyphicon glyphicon-home"}}
   :plugins [:contextmenu :dnd :types :sort :unique]})

(def jsk-node-type->route-name
  {:schedule :jsk.explorer/schedule
   :alert :jsk.explorer/alert
   :agent :jsk.explorer/agent
   :job :jsk.explorer/job
   :workflow :jsk.explorer/workflow})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tree Event Handling
(defn activate-handler
  [event data]
  (let [{:keys [id parent type jsk-node-type] :as node} (tree/original-node (tree/normalize-node (-> data .-node)))
        route-name (jsk-node-type->route-name (keyword jsk-node-type))
        route-url (when-not (tree/root-id? parent)
                    (routes/url-for route-name :id id))]
    ;(log/infof "node: %s, url: %s" node route-url)
    (some-> route-url routes/set-browser-url!)))

(defn move-handler
  "This gets called when a node is moved within the tree."
  [event data]
  (rf/dispatch [e/node-moved (tree/node-moved-data data)]))

(defn add-tree-listeners
  []
  (tree/register-activate-node-handler m/tree-sel activate-handler)
  (tree/register-move-node-handler m/tree-sel move-handler)
  ;; dnd listens on document not just the tree
  (tree/register-dnd-move-handler dnd/element-moved)
  (tree/register-dnd-stop-handler dnd/element-dropped))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Actual Views
(defn route->id
  [route]
  (get-in route [:route-params :id]))

(defmulti explorer-panels :handler)

(defmethod explorer-panels :jsk.explorer/agent [route]
  [agent-views/agent-editor (route->id route)])

(defmethod explorer-panels :jsk.explorer/alert [route]
  [alert-views/alert-editor (route->id route)])

(defmethod explorer-panels :jsk.explorer/job [route]
  [job-views/job-editor (route->id route)])

(defmethod explorer-panels :jsk.explorer/schedule [route]
  [schedule-views/schedule-editor (route->id route)])

(defmethod explorer-panels :default
  [route]
  [:h3 "Explorer"])

(defsnippet explorer* "templates/ui/data/explorer/explorer.html" [:.jsk-explorer]
  [tree-control main-content]
  {[:.jsk-explorer-tree] (k/content tree-control)
   [:.jsk-explorer-main] (k/content main-content)})

(defn explorer
  []
  (rf/dispatch [job-events/fetch-job-types])
  (let [tree-control [tree/js-tree m/tree-id tree-init-data add-tree-listeners]
        matched-panel (rf/subscribe [subs/explorer-view])]
    (fn []
      [explorer* tree-control (explorer-panels @matched-panel)])))
