(ns jsk.websockets
  (:require [taoensso.timbre :as log]
            [taoensso.sente :as sente]
            [compojure.api.sweet :refer [defroutes context POST GET PUT DELETE]]
            [ring.util.http-response :as http-response]
            [e85th.backend.web :as backend-web]
            [e85th.backend.websockets :as backend-ws]
            [e85th.commons.token :as token]))

(defn req->user-id
  [req]
  (get-in req [:identity :db/id]))

(defn request->user-identity
  "Answers with the user id who this request is associated with otherwise nil"
  [req res]
  (let [{:keys [token-factory]} res
        auth-token (get-in req [:params :auth-token])]
    (when (seq auth-token)
      (when-let [identity-data (token/token->data token-factory auth-token)]
        (log/debugf "identity-data is %s" identity-data)
        identity-data))))

(defn auth-user
  "Updates the request with the :identity key if the request parameter token is a valid s2 auth token."
  [req res]
  (let [identity-data (request->user-identity req res)]
    (if identity-data
      [true (assoc-in req [:identity] identity-data)]
      [false req])))

(defroutes ws-routes
  (context "/v1/chsk" [] :tags []
    :components [res]
    (GET "/" req
      (let [[valid-user? req] (auth-user req res)]
        (if valid-user?
          (backend-ws/do-get (:ws res) req)
          (http-response/unauthorized {:error "Not authenticated."}))))

    (POST "/" req
      (let [[valid-user? req] (auth-user req res)]
        (if valid-user?
          (backend-ws/do-get (:ws res) req)
          (http-response/unauthorized {:error "Not authenticated."}))))))
