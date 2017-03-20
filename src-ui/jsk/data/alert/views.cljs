(ns jsk.data.alert.views
  (:require [devcards.core :as d :refer-macros [defcard-rg]]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.rf.inputs :as inputs]
            [e85th.ui.net.rpc :as rpc]
            [e85th.ui.util :as u]
            [jsk.data.alert.events :as e]
            [jsk.data.alert.subs :as subs]
            [jsk.common.data :as data]
            [jsk.net.api :as api]
            [jsk.routes :as routes]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; -- Alert Channels

(defn format-channel-suggestion
  [[id identifier fname lname]]
  (str fname " " lname " (" identifier ")"))

(defsnippet alert-channel-item "templates/ui/data/alert/edit.html" [:.jsk-alert-channel-list [:.jsk-alert-channel-item first-child]]
  [{:keys [:channel/id :channel/identifier :user/first-name :user/last-name]}]
  {[:.jsk-alert-channel-item] (k/set-attr :key id)
   [:.jsk-alert-user] (k/content (str first-name " " last-name))
   [:.jsk-alert-channel-identifier] (k/content identifier)
   [:.jsk-alert-channel-delete] (k/listen :on-click #(rf/dispatch [e/dissoc-alert-channel id]))})

(defsnippet alert-channel-list* "templates/ui/data/alert/edit.html" [:.jsk-alert-channel-list]
  [channels]
  {[:.jsk-alert-channel-suggest] (k/substitute [inputs/awesomplete subs/channel-suggestions e/channel-suggestion-text-changed e/channel-suggestion-selected
                                                {:format-fn format-channel-suggestion :placeholder "Search channels ..." :clear-input-on-select? true}])
   [:.jsk-alert-channel-items] (k/content (map alert-channel-item channels))})


(defn alert-channel-list
  []
  (let [channels (rf/subscribe [subs/current-channels])]
    [alert-channel-list* @channels]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; -- Alert Editor
(defsnippet alert-view* "templates/ui/data/alert/edit.html" [:.jsk-alert-edit]
  []
  {[:.alert-name] (k/substitute [inputs/std-text subs/current-name e/name-changed])
   [:.alert-desc] (k/substitute [inputs/std-text subs/current-desc e/desc-changed])
   [:.alert-save-btn] (k/substitute [inputs/button subs/busy? e/save-alert "Save"])})

(defsnippet alert-editor-layout* "templates/ui/data/alert/edit.html" [:.jsk-alert-edit-layout]
  []
  {[:#jsk-alert-details-section] (k/content [alert-view*])
   [:#jsk-alert-channels-section] (k/content [alert-channel-list])})


(defn alert-editor
  ([id]
   (rf/dispatch [e/fetch-alert id])
   [alert-editor])
  ([]
   [alert-editor-layout*]))
