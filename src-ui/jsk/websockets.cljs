(ns jsk.websockets
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [e85th.ui.net.websockets :as websockets]
            [jsk.common.data :as data]
            [re-frame.core :as rf]))

(defmulti on-event first)

(defmethod on-event :default
  [msg]
  (log/infof "Ignoring message: %s" msg))

(defn init!
  "Call only after data/jsk-token is available."
  []
  (websockets/register-listener! on-event)
  (websockets/init! "/api/v1/chsk" {:type :auto
                                    :host (data/ws-host)
                                    :params {:token (data/jsk-token)}}))
