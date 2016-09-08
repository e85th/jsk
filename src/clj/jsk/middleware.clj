(ns jsk.middleware
  (:require [taoensso.timbre :as log]
            [schema.utils :as su]
            [ring.util.http-response :as http-response]
            [compojure.api.exception :as compojure-ex]
            [e85th.commons.util :as u]
            [e85th.backend.web :as web]))


(defn- with-user-permissions
  "Merges in user permissions if the user is logged in."
  [request resources]
  (let [{:keys [id]} (:identity request)
        permissions nil ;(some->> request :identity :id (user/find-user-permissions resources))
        permissions (or permissions {})]
    (update-in request [:identity] merge permissions)))

(defn wrap-user-permissions
  "Assoc user permissions into :identity :"
  [handler resources]
  (fn [request]
    (handler (with-user-permissions request resources))))

(defn wrap-user
  [handler resources]
  (fn [request]
    (let [request (assoc request :identity {:id 1 :permissions #{} :roles #{}})]
      (handler request))))
