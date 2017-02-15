(ns jsk.routes
  (:require [compojure.api.sweet :refer [defapi defroutes context GET POST ANY]]
            [compojure.api.sweet :as compojure-api]
            [ring.util.http-response :as http-response]
            [jsk.common.util :as util]
            [jsk.middleware :as middleware]
            [jsk.data.routes :as data]
            [ring.middleware.params :as ring-params]
            [ring.middleware.cookies :as ring-cookies]
            [compojure.route :as route]
            [e85th.commons.util :as u]
            [e85th.backend.middleware :as backend-mw]
            [e85th.backend.web :as backend-web]
            [taoensso.timbre :as log]
            [schema.core :as s]))

;; This is invoked by routes with :auth params specified
(defmethod backend-web/authorized? :standard route-authorization
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

(defroutes system-routes
  (context "" [] :tags ["system"]
    (GET "/version" []
      :summary "Gets the current version"
      (backend-web/text-response (util/build-properties)))
    (ANY "/echo" []
      :summary "Echo current request."
      (http-response/ok (backend-web/raw-request +compojure-api-request+)))))

(defn exception-handler
  [f]
  (backend-mw/wrap-api-exception-handling
   f
   (constantly nil)))

(defapi all-api-routes
  {:coercion (constantly backend-mw/coercion-matchers)
   :exceptions {:handlers {:compojure.api.exception/default backend-mw/error-actions}}
   :swagger {:ui "/"
             :spec "/swagger.json"
             :data {:info {:title "JSK APIs"}}}}



  ;; using var quote #' to facilitate changing route definitions during development
  (compojure-api/middleware
   [exception-handler]
   (context "/api" []
       #'system-routes
       #'data/agent-routes
       #'data/alert-routes
       #'data/schedule-routes
       #'data/tag-routes
       #'data/job-routes
       #'data/workflow-routes
       ))

  (compojure-api/undocumented
   (route/files "/" )
   (route/resources "/")

   ;; 404 route has to be last
   (fn [req]
     (http-response/not-found {:error "Unknown jsk resource."}))))

(defn make-handler
  [app-resources]
  (-> all-api-routes
      (compojure.api.middleware/wrap-components {:res app-resources})
      (middleware/wrap-user-permissions app-resources)
      (middleware/wrap-user app-resources)
      backend-mw/wrap-api-key-in-header
      backend-mw/wrap-cors
      ring-params/wrap-params
      ring-cookies/wrap-cookies
      backend-mw/wrap-swagger-remove-content-length))
