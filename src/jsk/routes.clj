(ns jsk.routes
  (:require [compojure.api.sweet :refer [defapi defroutes context GET POST ANY]]
            [compojure.api.sweet :as compojure-api]
            [ring.util.http-response :as http-response]
            [jsk.common.util :as util]
            [jsk.middleware :as middleware]
            [jsk.websockets :as websockets]
            [jsk.data.routes :as data]
            [ring.middleware.params :as ring-params]
            [ring.middleware.cookies :as ring-cookies]
            [compojure.route :as route]
            [e85th.backend.web :as web]
            [e85th.commons.util :as u]
            [e85th.backend.middleware :as backend-mw]
            [jsk.ui.core :as ui]
            [jsk.common.conf :as conf]
            [taoensso.timbre :as log]
            [schema.core :as s]))

;; This is invoked by routes with :auth params specified
(defmethod web/authorized? :standard route-authorization
  [{:keys [user permission auth-fn request]}]
  (assert user "user should always be available.")
  (let [permission-set (:permissions user)
        permission? (if permission
                      (some? (permission-set permission))
                      true)
        additional-auth? (if auth-fn (auth-fn) true)]
    (if (and permission? additional-auth?)
      [true "Allowed"]
      [false "Not authorized."])))


(defn handle-successful-login
  [res {:keys [user roles token] :as user-info} request]
  (let [server-name (web/request-server-name request)
        host (web/request-host request)
        cookie-opts {:domain server-name  :path "/" :max-age (* 60 60 24 1) :secure true}]
    (-> (http-response/see-other host)
        (web/set-cookie conf/jsk-token-name token cookie-opts))))

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
   "/"
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
    (GET "/login" []
         (web/html-response (ui/login-page res))
         ;; render login page
         )

    (GET "/" []
         (if (ui/authed? res +compojure-api-request+)
           (web/html-response (ui/main-page res))
           (http-response/see-other "/login")))))

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
       #'data/workflow-routes
       #'data/user-routes
       (compojure-api/undocumented
        (ANY "/echo" []
          :summary "Echo current request."
          (http-response/ok (web/raw-request +compojure-api-request+)))
        #'websockets/ws-routes)))

  (compojure-api/middleware
   [ui-exception-handler]
   (compojure-api/undocumented

    #'ui-routes))

  (compojure-api/undocumented
   (route/files "/" )
   (route/resources "/")

   ;; 404 route has to be last
   (fn [req]
     (http-response/not-found {:error "Unknown jsk resource."}))))

(defn make-handler
  [app-resources]
  (-> all-api-routes
      (backend-mw/wrap-cookie-value-in-components :res conf/jsk-token-name :bearer)
      (compojure.api.middleware/wrap-components {:res app-resources})
      (middleware/wrap-user-permissions app-resources)
      (middleware/wrap-user app-resources)
      backend-mw/wrap-api-key-in-header
      backend-mw/wrap-cors
      ring-params/wrap-params
      ring-cookies/wrap-cookies
      backend-mw/wrap-swagger-remove-content-length))
