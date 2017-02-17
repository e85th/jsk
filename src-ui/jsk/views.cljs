(ns jsk.views
  (:require [re-frame.core :as rf]
            [jsk.subs :as subs]
            [jsk.events :as e]
            [jsk.routes :as routes]
            [taoensso.timbre :as log]
            [jsk.login.views :as login-views]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]))


(defsnippet menu-bar "templates/ui/main.html" [:nav]
  [user]
  {[:.home-page-link] (k/set-attr :href (routes/url-for :jsk/home))
   [:#logout] (k/listen :on-click #(rf/dispatch [e/logout]))
   [:.user-first-name] (k/content (:first-name user))})

(defsnippet main-section "templates/ui/main.html" [:#jsk-all]
  [view user]
  {[:nav] (k/substitute [menu-bar user])
   [:main] (k/content view)})

(defn welcome-page
  []
  [:h1 "Hello JSK!"])

(defmulti panels :handler)



(defmethod panels :jsk/home [_] [welcome-page])
(defmethod panels :jsk/login [_] [login-views/login-panel])
(defmethod panels :default [_] [login-views/login-panel])

(def panel->enclosure
  {:jsk/login :div})

(defn main-panel
  []
  (let [matched-panel (rf/subscribe [subs/current-view])
        user (rf/subscribe [subs/current-user])]
    (fn []
      (log/infof "matched-panel: %s" @matched-panel)
      (let [view (panels @matched-panel)
            enclosure (get panel->enclosure @matched-panel main-section)]
        [enclosure view @user]))))
