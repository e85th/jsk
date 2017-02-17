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
  {[:.email] (k/substitute [inputs/std-text subs/email e/email-changed])
   [:.password] (k/substitute [inputs/std-password subs/password e/password-changed])
   [:.login-btn] (k/substitute [inputs/button subs/login-busy? e/email-pass-auth "Login"])})


(defn login-panel
  []
  [login-view])
