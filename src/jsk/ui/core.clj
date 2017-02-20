(ns jsk.ui.core
  (:require [net.cgrand.enlive-html :as h]
            [net.cgrand.jsoup :as jsoup]
            [jsk.common.conf :as conf]
            [schema.core :as s]
            [clj-time.format :as t-fmt]
            [cemerick.url :as url]
            [e85th.commons.net.url :as e85-url]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [jsk.data.settings :as settings]
            [jsk.data.user :as user]
            [e85th.commons.ex :as ex]
            [e85th.backend.web :as web])
  (:import [e85th.commons.exceptions AuthExceptionInfo]))

(h/set-ns-parser! jsoup/parser)

(h/deftemplate main-page* "templates/server/main.html"
  [version component-name api-host login-url google-oauth-client-id]
  [:#google-signin-client-id] (h/set-attr :content google-oauth-client-id)
  [:#jsk-js] (h/replace-vars {:version version})
  [:#api-host] (h/set-attr :value api-host)
  [:#login-url] (h/set-attr :value login-url)
  [:#init-component] (h/set-attr :value component-name))

;; FIXME: Get actual data
(defn fetch-user-details
  [res]
  ({:roles #{:jsk/admin}}))

(s/defn ensure-admin-role!
  [res]
  (let [{:keys [roles] :as user-info} (fetch-user-details res)]
    (log/infof "roles are: %s" roles)
    (when-not (roles :jsk/admin)
      (throw (ex/new-auth-exception :invalid-permission "Not an admin.")))))

(defn append-redirect-url
  [base-url redirect-url]
  (-> (url/url base-url)
      (assoc-in [:query "sign-in-success-url"] redirect-url)
      str))

(defn main-page
  [{:keys [version sys-config] :as res}]
  ;; FIXME: login-url should be dynamically generated
  (let [site-url (:setting.site/url (settings/find-site res))
        login-url (str site-url "/login") ;; FIXME: this is bad
        client-id (settings/find-google-auth-client-id res)]
    (apply str (main-page* version "main" site-url login-url client-id))))


(h/deftemplate login-page-view "templates/server/login.html"
  [version component-name api-host login-url google-oauth-client-id]
  [:#google-signin-client-id] (h/set-attr :content google-oauth-client-id)
  [:#jsk-js] (h/replace-vars {:version version})
  [:#api-host] (h/set-attr :value api-host)
  [:#login-url] (h/set-attr :value login-url)
  [:#init-component] (h/set-attr :value component-name))


(defn login-page
  [{:keys [version sys-config] :as res}]
  (let [site-url (:setting.site/url (settings/find-site res))
        login-url (str site-url "/login") ;; FIXME: this is bad
        client-id (settings/find-google-auth-client-id res)]
    (apply str (login-page-view version "login" site-url login-url client-id))))

(s/defn authed?
  "Answers true if authenticated authorized by looking up the user in the db."
  [res request]
  (try
    (some->> (web/cookie-value request conf/jsk-token-name)
             (user/token->user res)
             some?)
    (catch AuthExceptionInfo ex
      (log/info (str ex)))
    (catch Exception ex
      (log/error ex)
      false)))
