(ns jsk.subscriber
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [schema.core :as s]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [jsk.common.artemis :as artemis]
            [e85th.backend.websockets :as backend-ws]
            [e85th.commons.util :as u]
            [e85th.commons.mq :as mq]))

;; will need heartbeats to determine if agent is dead
;; if no response from agent in over some threshold mark the agent as dead
;; fail any new jobs that might go to that agent


;; agent to director: always send messages to director. director can process when it recovers

;; UI should display status of director, and agents
;; (comp f edn/read-string artemis/message-body-string)

;; create a temporary queue from the address and relays to the websockets
(defn dynamic-queue-name
  [context]
  (let [hostname (str/replace (u/hostname) #"\." "-")]
    (str context "-" hostname "-" (u/uuid))))

(def ^{:doc "Receives an artemis message, reads the body string and returns it as a clojure data structure"}
  read-message (comp edn/read-string artemis/message-body-string))

(defn- on-message
  [handler msg]
  (try
    (handler (read-message msg))
    (artemis/ack msg)
    (catch Exception ex
      (log/error ex))))

(defn- init-subscriber
  [subscriber address q-name msg-handler-fn]
  (let [f (artemis/client-session-factory)
        session (artemis/create-session f true true 0)
        _ (artemis/create-temporary-queue session address q-name)
        consumer (artemis/create-consumer session q-name)]
    (artemis/set-message-handler consumer (partial on-message msg-handler-fn))
    (artemis/start-session session)
    (assoc subscriber :session-factory f :session session :consumer consumer)))

(defn- dispose-subscriber
  [s]
  (some-> s :consumer artemis/close)
  (some-> s :session artemis/stop-session)
  (some-> s :session artemis/close)
  (some-> s :session-factory artemis/close)
  (dissoc s :consumer :session :session-factory))

(defrecord ConsoleSubscriber [ws address]
  component/Lifecycle
  (start [this]
    (let [q-name (dynamic-queue-name "jsk.console")]
      (log/infof "ConsoleSubscriber starting for queue: %s" q-name)
      (-> (init-subscriber this address q-name (partial backend-ws/broadcast! ws))
          (assoc :q-name q-name))))

  (stop [this]
    (log/infof "ConsoleSubscriber stopping for queue: %s." (:q-name this))
    (dispose-subscriber this)))


(defn new-console-subscriber
  [address]
  (map->ConsoleSubscriber {:address address}))

(defn on-agent-msg
  [res msg]
  (log/infof "TODO: handle agent msg received. Msg: %s" msg))

;; read from the queue and invoke a multi method for agent to handle message commands
;; res are the resources
(defrecord AgentSubscriber [agent-resources agent-name]

  component/Lifecycle
  (start [this]
    (let [address (str "jsk.agents." agent-name)]
      (log/infof "AgentSubscriber starting for agent: %s" agent-name)
      (init-subscriber this address (dynamic-queue-name address) (partial on-agent-msg agent-resources))))

  (stop [this]
    (log/infof "AgentSubscriber stopping for agent: %s" agent-name)
    (dispose-subscriber this)))

(defn new-agent-subscriber
  [agent-name]
  (map->AgentSubscriber {:agent-name agent-name}))

;; read from queue and invoke a multi method for director to handle message commands
;; res are the resources
(defn on-director-msg
  [res msg]
  (log/infof "TODO: handle director msg received. Msg: %s" msg))

(defrecord DirectorSubscriber [director-resources address]
  component/Lifecycle
  (start [this]
    (log/infof "DirectorSubscriber starting for address: %s" address)
    (init-subscriber this address (dynamic-queue-name "jsk.director") (partial on-director-msg director-resources)))

  (stop [this]
    (log/infof "DirectorSubscriber stopping for address: %s" address)
    (dispose-subscriber this)))

(defn new-director-subscriber
  [address]
  (map->DirectorSubscriber {:address address}))
