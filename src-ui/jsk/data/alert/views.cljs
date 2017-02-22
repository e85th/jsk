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

(defn new-channel-typeahead-config
  "Creates dataset and typeahead opts for user search."
  []
  (let [remote-url (api/suggest-url)
        wildcard "%QUERY"
        prep-fn (fn [search settings]
                  ;; jquery xhr settings that bloodhound works with
                  (clj->js (-> {:url remote-url
                                :contentType "application/json"
                                :data #js {:q search}
                                :type "GET"
                                :dataType "json"}
                               (rpc/with-bearer-auth (data/jsk-token)))))
        bloodhound (inputs/new-bloodhound remote-url wildcard prep-fn)
        display-fn (fn [[chan-id identifier first-name last-name :as suggestion]]
                     (log/infof "chan-id %s, first-name: %s" chan-id first-name)
                     (str "<div>" first-name " " last-name "(" identifier ")</div>"))]
    {:dataset #js {:name "channels-dataset"
                   :source bloodhound
                   :templates #js {:suggestion display-fn}}
     :typeahead-opts {:minLength 1 :highlight true}}))

(defn new-channel-typeahead
  []
  (let [{:keys [dataset typeahead-opts]} (new-channel-typeahead-config)]
    [inputs/typeahead nil e/channel-selected {:placeholder "Channel Search"} typeahead-opts dataset]))

(defn ac
  []
  [inputs/autocomplete (api/suggest-url) {"Authorization" (str "Bearer " (data/jsk-token))} subs/ac e/channel-selected])

(defsnippet alert-actions* "templates/ui/data/alert/list.html" [:.jsk-alert-action-bar]
  []
  {[:.jsk-new-alert-action] (k/listen :on-click #(rf/dispatch [e/new-alert]))})

(defsnippet alert-item* "templates/ui/data/alert/list.html" [:.jsk-alert-list [:.jsk-alert-item first-child]]
  [{:keys [db/id alert/name]}]
  {[:.jsk-alert-item] (k/do->
                       (k/set-attr :key id :href (routes/url-for :jsk.explorer/alert :id id))
                       (k/content name))})

(defsnippet alert-list* "templates/ui/data/alert/list.html" [:.jsk-alert-list]
  [alerts]
  {[:.jsk-alert-list] (k/content (map alert-item* alerts))})


(defn alert-list
  []
  (rf/dispatch [e/fetch-alert-list])
  (let [alerts (rf/subscribe [subs/alert-list])]
    (fn []
      [alert-list* @alerts])))

(defn alert-list-with-actions
  []
  [:div
   [alert-actions*]
   [alert-list]])

(defsnippet alert-view* "templates/ui/data/alert/edit.html" [:.jsk-alert-edit]
  []
  {[:.alert-name] (k/substitute [inputs/std-text subs/current-name e/name-changed])
   [:.alert-desc] (k/substitute [inputs/std-text subs/current-desc e/desc-changed])
   [:.alert-save-btn] (k/substitute [inputs/button subs/busy? e/save-alert "Save"])})

;; -- Alert Channels
(defsnippet alert-channel-item "templates/ui/data/alert/edit.html" [:.jsk-alert-channel-list [:.jsk-alert-channel-item first-child]]
  [alert-id [chan-id identifier first-name last-name]]
  {[:.jsk-alert-channel-item] (k/set-attr :key chan-id)
   [:.jsk-alert-user] (k/content (str first-name last-name))
   [:.jsk-alert-channel-identifier] (k/content identifier)
   [:.jsk-alert-channel-delete] (k/listen :on-click #(rf/dispatch [e/dissoc-channel chan-id]))})

(defsnippet alert-channel-list* "templates/ui/data/alert/edit.html" [:.jsk-alert-channel-list]
  [channels]
  {[:.jsk-alert-channel-suggest] (k/substitute (new-channel-typeahead))
   [:.add-channel-btn] (k/substitute [inputs/button e/assoc-channel "Add"])
   [:.jsk-alert-channel-items] (k/content (map alert-channel-item channels))})

(defn alert-channel-list
  []
  (let [addresses (rf/subscribe [subs/current-channels])]
    [alert-channel-list* @addresses]))

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
