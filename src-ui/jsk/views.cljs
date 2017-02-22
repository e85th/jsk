(ns jsk.views
  (:require [re-frame.core :as rf]
            [jsk.subs :as subs]
            [jsk.events :as e]
            [jsk.routes :as routes]
            [e85th.ui.rf.inputs :as inputs]
            [taoensso.timbre :as log]
            [jsk.login.views :as login-views]
            [jsk.login.events :as login-events]
            [jsk.data.agent.views :as agent-views]
            [jsk.data.alert.views :as alert-views]
            [jsk.data.schedule.views :as schedule-views]
            [jsk.data.explorer.views :as explorer-views]
            [jsk.common.data :as data]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]))


(defsnippet menu-bar "templates/ui/main.html" [:nav]
  [user]
  {[:.home-page-link] (k/set-attr :href (routes/url-for :jsk/home))
   [:#jsk-explorer] (k/set-attr :href (routes/url-for :jsk/explorer))
   [:#logout] (k/listen :on-click #(rf/dispatch [login-events/logout]))
   [:.user-first-name] (k/content (:user/first-name user))})


(defsnippet main-section "templates/ui/main.html" [:#jsk-all]
  [view user]
  {[:nav] (k/substitute [menu-bar user])
   [:main] (k/content view)})

(defn welcome-page
  []
  [:div
   [:h1 "Hello JSK!"]])

(defmulti panels :handler)

(defmethod panels :jsk/home [_] [welcome-page])
(defmethod panels :jsk/login [_] [login-views/login-panel])

;; -- Explorer
(defmethod panels :jsk/explorer [_] [explorer-views/explorer])

;; -- Agents

(defmethod panels :default
  [{:keys [handler]}]
  [:h3 (str "No view configured for handler: " handler)])

(def panel->enclosure
  {:jsk/login :div})

(defn main-panel
  []
  (let [matched-panel (rf/subscribe [subs/main-view])
        user (rf/subscribe [subs/current-user])]
    (fn []
      (log/infof "matched-panel: %s" @matched-panel)
      (let [view (panels @matched-panel)
            enclosure (get panel->enclosure @matched-panel main-section)]
        [enclosure view @user]))))
