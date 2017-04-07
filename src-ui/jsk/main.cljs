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
    (routes/set-browser-url! (routes/url-for :jsk/login))))


(defn mount-root!
  []
  (reagent/render [v/main-panel] (dom/element-by-id "app")))

(defn enable-dev-settings
  []
  ;(re-frisk.core/enable-re-frisk!)
  )

(defn auth-err-handler
  [response]
  (routes/set-browser-url! (routes/url-for :jsk/login))
  {})

(defn init
  []
  (routes/init!)
  (api/set-auth-err-handler! auth-err-handler)
  (s/set-fn-validation! true)
  (data/set-api-host! (dom/element-value "api-host"))
  (rf/dispatch [e/fetch-user]) ;; if not logged in then this will trigger login

  (when ^boolean js/goog.DEBUG
    (enable-dev-settings))

  (mount-root!)
  (websockets/init!))

(set! (.-onload js/window) init)
