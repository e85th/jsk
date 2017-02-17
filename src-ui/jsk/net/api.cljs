(ns jsk.net.api
  (:require [e85th.ui.net.rpc :as rpc]
            [taoensso.timbre :as log]
            [jsk.common.data :as data]
            [re-frame.core :as rf]
            [schema.core :as s]
            [e85th.ui.util :as u]))

(s/defn ^:private full-url
  [url-path]
  (str (data/api-host) "/api" url-path))


(s/defn new-request
  ([method url ok err]
   (new-request method url {} ok err))
  ([method url params ok err]
   (-> (rpc/new-re-frame-request method (full-url url) params ok err)
       rpc/with-transit-format
       (rpc/with-bearer-auth (data/jsk-token)))))

(s/defn call!
  "For testing really. Use effects to actually make calls.
   re-frame handling. ok and err can be either keywords or a vector.
   If vector then the first should be a keyword to conform to re-frame dispatch
   semantics."
  ([method url params ok err]
   (call! (new-request method url params ok err)))
  ([req]
   (let [ensure-handler-fn (fn [{:keys [handler error-handler] :as r}]
                             (cond-> r
                               (vector? handler) (assoc :handler #(rf/dispatch (conj handler %)))
                               (vector? error-handler) (assoc :error-handler #(rf/dispatch (conj error-handler %)))))
         normalize (comp ensure-handler-fn)]
     (rpc/call (normalize req)))))

(s/defn authenticate
  "Generates a request map that can be executed by call!"
  [token :- s/Str ok err]
  (new-request :post "/v1/users/actions/authenticate" {:with-firebase {:token token}} ok err))

(def authenticate! (comp call! authenticate))

(s/defn google-authenticate
  "Generates a request map that can be executed by call!"
  [token :- s/Str ok err]
  (new-request :post "/v1/users/actions/authenticate" {:with-google {:token token}} ok err))
