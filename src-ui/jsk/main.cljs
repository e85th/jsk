(ns jsk.main
  (:require [taoensso.timbre :as log]
            [jsk.common.data :as data]
            [re-frame.core :as rf]
                                        ;[re-frisk.core :as re-frisk]
            [schema.core :as s]
            [reagent.core :as reagent]
            [jsk.net.api :as api]
            [day8.re-frame.http-fx]
            [hodgepodge.core :as hp]
            [jsk.websockets :as websockets]
            [jsk.events :as e]
            [jsk.views :as v]
            [jsk.login.views :as login-views]
            [jsk.login.events :as login-events]
            [jsk.routes :as routes]
            [e85th.ui.rf.fx]
            [e85th.ui.util :as u]
            [e85th.ui.dom :as dom]
            [e85th.ui.browser :as browser]
            [e85th.ui.edn-io]
            [e85th.ui.google-oauth :as google-oauth]))

(defn ^:export google-sign-in
  [g-user]
  (let [{:keys [token]} (google-oauth/google-user->map g-user)]
    (rf/dispatch [login-events/google-auth token])))

(defn verify-login
  []
  (log/info "verify-login " (data/jsk-token-set?))
  (when-not (data/jsk-token-set?)
    (browser/location (data/login-url))
    (rf/dispatch [e/set-main-view :jsk/login])))


(def name->component
  {"main" [v/main-panel]
   "login" [login-views/login-panel]})

(defn mount-root!
  [nm]
  (reagent/render (get name->component nm [:h2 "JSK: Unknown component"])
                  (dom/element-by-id "app")))

(defn enable-dev-settings
  []
  ;(re-frisk.core/enable-re-frisk!)
  )

(defn init
  []
  ;; FIXME: need a configurable way to set this
  (let [comp-nm (dom/element-value "init-component")
        login? (= "login" comp-nm)]

    (s/set-fn-validation! true)
    (data/set-api-host! (dom/element-value "api-host"))
    (data/set-login-url! (dom/element-value "login-url"))
    (routes/init!)

    (when (and (not login?) (data/jsk-token-set?))
      (rf/dispatch [e/fetch-user]))

    (when ^boolean js/goog.DEBUG
      (enable-dev-settings))

    (mount-root! comp-nm)

    (when-not login?
      (websockets/init!)
      (verify-login))))

(set! (.-onload js/window) init)
