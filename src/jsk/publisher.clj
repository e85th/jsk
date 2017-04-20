(ns jsk.publisher
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [schema.core :as s]
            [jsk.common.artemis :as artemis]
            [e85th.backend.websockets :as backend-ws]
            [e85th.commons.mq :as mq]))



;; This pushes updates to the web clients and the director's queue.
(defrecord WebConsoleMessagePublisher [ws director-publisher]
  component/Lifecycle
  (start [this]
    (log/infof "WebConsoleMessagePublisher started.")
    this)
  (stop [this]
    (log/infof "WebConsoleMessagePublisher stopped.")
    this)

  mq/IMessagePublisher
  (publish [_ msg]
    (s/validate mq/Message msg)
    ;; push to director only, agents don't need to know about crud events
    ;; director does need to know to be able to reset schedule triggers
    (mq/publish director-publisher msg)
    ;; push to ui
    (backend-ws/broadcast! ws msg)))

(s/defn new-web-console-publisher
  "ws is the WebSocket from e85th.backend.websockets component."
  ([]
   (map->WebConsoleMessagePublisher {}))
  ([ws director-publisher]
   (map->WebConsoleMessagePublisher {:ws ws :director-publisher director-publisher})))


(s/defn agent-address :- s/Str
  [agent-name :- s/Str]
  (str "jsk.agents." agent-name))

(defn- send-message-to-agent
  "Makes sure there's a producer to target the jsk agent and send the message."
  [{:keys [session address->producer] :as state} [msg-type body :as msg]]
  ;; from the message get the jsk agent name to send the message to
  ;; generate an address for the agent-name
  ;; see if address->producer has a producer already otherwise create one
  ;; create artemis message and send
  (let [address (-> body :jsk.agent/name agent-address)
        address->producer (if (address->producer address)
                            address->producer
                            (assoc address->producer address (-> (artemis/new-message-producer session address)
                                                                 component/start)))]
    (mq/publish (address->producer address) msg)
    (assoc state :address->producer address->producer)))

(defrecord AgentPublisher []

  component/Lifecycle
  (start [this]
    ;; create session
    (let [factory (artemis/client-session-factory)
          session (artemis/create-session factory true true 0)]
      (artemis/start-session session)
      (log/infof "AgentPublisher started.")
      (assoc this :agent (agent {:session session
                                 :session-factory factory
                                 :address->producer {}}))))
  (stop [this]
    (let [{:keys [session-factory session address->producer]} (some-> this :agent deref)]
      (doall (map component/stop (vals address->producer)))
      (some-> session artemis/stop-session)
      (some-> session artemis/close)
      (some-> session-factory artemis/close)
      (log/infof "AgentPublisher stopped.")
      (dissoc this :agent)))

  mq/IMessagePublisher
  (publish [this msg]
    (s/validate mq/Message msg)
    (send-off (:agent this) send-message-to-agent msg)))


(defn new-agent-publisher
  []
  (map->AgentPublisher {}))



(defn- send-message-to-consoles
  [{:keys [producer] :as state} msg]
  (mq/publish producer msg)
  state)


;; This exists to be able to reuse the session when publishing data to consoles.
;; It's possible that the director code is invoked by inbound messages running in different threads
;; and there could be a race condition when accessing the session. Using a clojure agent
;; queues messages for the agent thread and are processed in order.
(defrecord ConsolePublisher [address]

  component/Lifecycle
  (start [this]
    ;; create session
    (let [factory (artemis/client-session-factory)
          session (artemis/create-session factory true true 0)
          producer (component/start (artemis/new-message-producer session address))]
      (artemis/start-session session)
      (log/infof "ConsolePublisher started.")
      (assoc this :agent (agent {:session session
                                 :session-factory factory
                                 :producer producer}))))

  (stop [this]
    (let [{:keys [session session-factory producer]} (some-> this :agent deref)]
      (some-> producer component/stop)
      (some-> session artemis/stop-session)
      (some-> session artemis/close)
      (some-> session-factory artemis/close)
      (log/infof "ConsolePublisher stopped.")
      (dissoc this :agent)))

  mq/IMessagePublisher
  (publish [this msg]
    (send-off (:agent this) send-message-to-consoles msg)))


(defn new-console-publisher
  [address]
  (map->ConsolePublisher {:address address}))
