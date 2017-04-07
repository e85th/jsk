(ns jsk.routes
  (:require [compojure.api.sweet :refer [defapi defroutes context GET POST ANY]]
            [compojure.api.sweet :as compojure-api]
            [ring.util.http-response :as http-response]
            [jsk.common.util :as util]
            [jsk.websockets :as websockets]
            [jsk.data.routes :as data]
            [ring.middleware.params :as ring-params]
            [ring.middleware.cookies :as ring-cookies]
            [compojure.route :as route]
            [e85th.backend.web :as web]
            [e85th.commons.token :as token]
            [e85th.commons.util :as u]
            [e85th.backend.middleware :as backend-mw]
            [buddy.auth.middleware :as buddy-auth-mw]
            [jsk.data.user :as user]
            [jsk.ui.core :as ui]
            [jsk.common.conf :as conf]
            [taoensso.timbre :as log]
            [schema.core :as s]))

;; This is invoked by routes with :auth params specified
(defmethod web/authorized? :standard route-authorization
  [{:keys [user permission auth-fn request]}]
  (assert user "user should always be available.")
  (if user ;; just checking if logged in
    [true "Allowed"]
    [false "Not authorized."]))


(defn wrap-user-auth
  [handler res]
  (fn [{buddy-user :identity :as req}]
    (let [req (cond-> req
                buddy-user (update-in [:identity] merge (user/find-user-auth res (:db/id buddy-user))))]
      (handler req))))

(defroutes system-routes
  (context "" [] :tags ["system"]
    (GET "/version" []
      :summary "Gets the current version"
      (web/text-response (util/build-properties)))
    (ANY "/echo" []
      :summary "Echo current request."
      (http-response/ok (web/raw-request +compojure-api-request+)))))

(defn api-exception-handler
  [f]
  (backend-mw/wrap-api-exception-handling
   f
   (constantly nil)))

(defn ui-exception-handler
  [f]
  (backend-mw/wrap-site-exception-handling
   f
   "/login"
   (constantly nil)))

(defroutes system-routes
  (context "" [] :tags ["system"]
    :components [res]
    (GET "/_/version" []
      :summary "Gets the current version"
      (web/text-response (util/build-properties)))))


(defroutes ui-routes
  (context "" [] :tags ["ui"]
    :components [res]
    #_(GET "/login" []
      (web/html-response (ui/login-page res)))

    (GET "/" []
      (http-response/see-other "/jsk"))

    (GET "/jsk*" []
      (web/html-response (ui/main-page res)))))

(defapi all-api-routes
  {:coercion (constantly backend-mw/coercion-matchers)
   :exceptions {:handlers {:compojure.api.exception/default backend-mw/error-actions}}
   :swagger {:ui "/swagger"
             :spec "/swagger.json"
             :data {:info {:title "JSK APIs"}}}}

  ;; using var quote #' to facilitate changing route definitions during development
  (compojure-api/middleware
   [api-exception-handler]
   (context "/api" []
       #'system-routes
       #'data/agent-routes
       #'data/alert-routes
       #'data/schedule-routes
       #'data/tag-routes
       #'data/job-routes
       #'data/job-type-routes
       #'data/workflow-routes
       #'data/user-routes
       #'data/search-routes
       #'data/explorer-routes
       (compojure-api/undocumented
        (ANY "/echo" []
          :summary "Echo current request."
          (http-response/ok (web/raw-request +compojure-api-request+)))
        #'websockets/ws-routes)))

  (compojure-api/undocumented
   #'ui-routes
   (route/files "/" )
   (route/resources "/")

   ;; 404 route has to be last
   (fn [req]
     (http-response/not-found {:error "Unknown jsk resource."}))))

(defn make-handler
  [{:keys [token-factory] :as app}]
  (-> all-api-routes
      (wrap-user-auth app)
      (compojure.api.middleware/wrap-components {:res app})
      (buddy-auth-mw/wrap-authentication (token/backend token-factory))
      backend-mw/wrap-api-key-in-header
      backend-mw/wrap-cors
      ring-params/wrap-params
      ring-cookies/wrap-cookies
      backend-mw/wrap-swagger-remove-content-length))
