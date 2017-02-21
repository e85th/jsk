(ns jsk.publisher
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [schema.core :as s]
            [e85th.backend.websockets :as backend-ws]
            [e85th.commons.mq :as mq]))



;; Right now just publishing to WebSockets
;; Will need a StandaloneMessagePublisher to push to ws and conductor
(defrecord WebServerMessagePublisher [ws]
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  mq/IMessagePublisher
  (publish [this msg]
    (s/validate mq/Message msg)
    (backend-ws/broadcast! ws msg)))

(s/defn new-web-server-publisher
  "ws is the WebSocket from e85th.backend.websockets component."
  ([]
   (map->WebServerMessagePublisher {}))
  ([ws]
   (map->WebServerMessagePublisher {:ws ws})))
