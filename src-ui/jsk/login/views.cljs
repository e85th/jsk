(ns jsk.login.views
  (:require [devcards.core :as d :refer-macros [defcard-rg]]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]
            [reagent.core :as reagent]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.rf.inputs :as inputs]
            [jsk.routes :as routes]
            [jsk.login.subs :as subs]
            [jsk.login.events :as e]))


(defsnippet login-view "templates/ui/login.html" [:.login-window]
  []
  )

(defn on-success
  [x]
  (log/infof "Omg x"))

(defn on-fail
  []
  (log/infof "Omg fail x"))

(defn login-panel
  []
  (reagent/create-class
   {:display-name "login-panel"
    :reagent-render login-view
    :component-did-mount (fn []
                           (log/infof "do render of signin2")
                           (js/gapi.signin2.render "g-signin2" #js {"scope" "profile email"
                                                                    "width" 240
                                                                    "height" 50
                                                                    "longtitle" true
                                                                    "theme" "dark"
                                                                    "onsuccess" on-success
                                                                    "onfailure" on-fail}))}))
