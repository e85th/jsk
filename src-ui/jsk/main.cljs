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
            [jsk.routes]
            [e85th.ui.rf.fx]
            [e85th.ui.util :as u]
            [e85th.ui.edn-io]
            [e85th.ui.google-oauth :as google-oauth]))

(defn ^:export google-sign-in
  [g-user]
  (let [{:keys [token]} (google-oauth/google-user->map g-user)]
    (rf/dispatch [e/google-auth token])))

(defn verify-login
  []
  (log/info "verify-login " (data/jsk-token-set?))
  (when-not (data/jsk-token-set?)
    (u/set-window-location! (data/login-url))
    (rf/dispatch [e/set-current-view :jsk/login])))

(defn ensure-google-login
  [ensure-login login-url]
  (google-oauth/current-user-listen (fn [u]
                                        ;(log/infof "current-user-info: %s" u)
                                    (if-not u
                                      (rf/dispatch [e/set-current-view :login])
                                      (ensure-login u)))))


(defn mount-root!
  []
  (reagent/render [v/main-panel]  (u/element-by-id "app")))

(defn enable-dev-settings
  []
  ;(re-frisk.core/enable-re-frisk!)
  )

(defn init
  []
  ;; FIXME: need a configurable way to set this
  (s/set-fn-validation! true)
  (data/set-api-host! (u/element-value "api-host"))
  (data/set-login-url! (u/element-value "login-url"))

  (when ^boolean js/goog.DEBUG
    (enable-dev-settings))
  (when-not (= "login" (u/element-value "init-component"))
    (websockets/init!)
    (mount-root!)
    (verify-login)))

(set! (.-onload js/window) init)
