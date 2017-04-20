(ns jsk.common.artemis
  (:refer-clojure :exclude [send])
  (:require [clojure.walk :as walk]
            [com.stuartsierra.component :as component]
            [clojure.edn :as edn]
            [e85th.commons.mq :as mq]
            [taoensso.timbre :as log])
  (:import [org.apache.activemq.artemis.api.core SimpleString TransportConfiguration]
           [org.apache.activemq.artemis.core.config Configuration]
           [org.apache.activemq.artemis.core.config.impl ConfigurationImpl]
           [org.apache.activemq.artemis.api.core.client ActiveMQClient ClientSessionFactory ClientSession ClientProducer ClientMessage ClientConsumer MessageHandler]
           [org.apache.activemq.artemis.core.server.embedded EmbeddedActiveMQ]
           [org.apache.activemq.artemis.core.server.impl ActiveMQServerImpl]
           [org.apache.activemq.artemis.core.remoting.impl.invm InVMConnectorFactory InVMAcceptorFactory]
           [org.apache.activemq.artemis.core.remoting.impl.netty NettyAcceptorFactory NettyConnectorFactory]
           [clojure.lang IFn]))


(defn ^Configuration config
  [opts]
  (doto (ConfigurationImpl.)
    (.setSecurityEnabled false)
    (.setAcceptorConfigurations #{(TransportConfiguration. "org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory" opts)
                                  (TransportConfiguration. "org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory")})))

(defn ^EmbeddedActiveMQ embedded-server
  [^Configuration config]
  (doto (EmbeddedActiveMQ.)
    (.setConfiguration config)))

(defn ^ClientSessionFactory client-session-factory
  []
  (-> [(TransportConfiguration. "org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory")
       (TransportConfiguration. "org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory")]
      into-array
      ActiveMQClient/createServerLocatorWithoutHA
      .createSessionFactory))

(defn ^ClientSession create-session
  "Creates and returns the ClientSession"
  ([^ClientSessionFactory session-factory]
   (.createSession session-factory))
  ([^ClientSessionFactory session-factory auto-commit-sends? auto-commit-acks? ack-batch-size]
   (.createSession session-factory auto-commit-sends? auto-commit-acks? ack-batch-size)))


(defn create-queue
  "Returns nil"
  ([^ClientSession session ^String address ^String q-name]
   (.createQueue session address q-name))
  ([^ClientSession session ^String address ^String q-name ^Boolean durable?]
   (.createQueue session address q-name durable?))
  ([^ClientSession session ^String address ^String q-name ^String filter-str ^Boolean durable?]
   (.createQueue session address q-name filter-str durable?)))

(defn create-temporary-queue
  [^ClientSession session ^String address ^String q-name]
  (.createTemporaryQueue session address q-name))

(defn ^ClientProducer create-producer
  [^ClientSession session ^String address]
  (.createProducer session address))

(defn ^ClientMessage create-message
  ([^ClientSession session ^Boolean durable?]
   (.createMessage session durable?))
  ([^ClientSession session ^Byte type ^Boolean durable? ^Long expiration ^Long timestamp ^Byte priority]
   (.createMessage session durable? expiration timestamp priority)))

(defn create-durable-message
  [^ClientSession session]
  (create-message session true))


(defn start-session
  [^ClientSession session]
  (.start session))

(defn commit-session
  [^ClientSession session]
  (.commit session))

(defn stop-session
  [^ClientSession session]
  (.stop session))

(defn close
  [^java.lang.AutoCloseable f]
  (.close f))

(defn create-consumer
  "Creates a ClientConsumer instance."
  ([^ClientSession session ^String q-name]
   (.createConsumer session q-name))
  ([^ClientSession session ^String q-name ^String filter-str]
   (.createConsumer session q-name filter-str))
  ([^ClientSession session ^String q-name ^String filter-str ^Integer window-size ^Integer max-rate ^Boolean browse-only?]
   (.createConsumer session q-name filter-str window-size max-rate browse-only?)))

(defn set-message-handler
  "Sets a callback on the consumer which is invoked when a message is available.
   f is a one arity function that recieves a ClientMessage."
  [^ClientConsumer consumer ^IFn f]
  (.setMessageHandler consumer (reify MessageHandler
                                 (onMessage [_ msg]
                                   (f msg)))))

(defn query-queue
  "Returns a QueryQueue instance."
  [^ClientSession session ^String q-name]
  (.queueQuery session (SimpleString. q-name)))

(defn message->map
  "Returns the message properties as a clojure map."
  [^ClientMessage msg]
  ;; keywordize-keys only works on clojure maps
  (some->> msg .toMap (into {}) walk/keywordize-keys))

(defn message-body-string
  "One arity returns the message body as a string.
   Two arity writes the body to the message body buffer."
  ([^ClientMessage msg ^String body]
   (.writeBodyBufferString msg body))
  ([^ClientMessage msg]
   (some-> msg .getBodyBufferDuplicate .readString)))

(defn send
  "Sends the message. Returns nil."
  [^ClientProducer producer ^ClientMessage msg]
  (.send producer msg))

(defn ack
  [^ClientMessage msg]
  (.acknowledge msg))

(defrecord Server [active-mq]
  component/Lifecycle
  (start [this]
    (.start active-mq)
    (log/infof "ActiveMQ Server started.")
    this)

  (stop [this]
    (some-> active-mq .stop)
    (log/infof "ActiveMQ Server stopped.")
    this))

(defn new-server
  "Create an embedded server using InVMAcceptor and InVMConnector"
  ([]
   (new-server "0.0.0.0" 61616))
  ([host port]
   (let [active-mq (embedded-server (config {"host" host "port" port}))]
     (map->Server {:active-mq active-mq}))))

(defrecord QueueConsumer [q-name on-message-fn]
  component/Lifecycle
  (start [this]
    (let [f (client-session-factory)
          session (create-session f)
          consumer (create-consumer session q-name)]
      (set-message-handler consumer on-message-fn)
      (start-session session)
      (assoc this :session-factory f :session session :consumer consumer)))

  (stop [this]
    (some-> this :consumer close)
    (some-> this :session stop-session)
    (some-> this :session close)
    (some-> this :session-factory close)
    (dissoc this :consumer :session :session-factory)))

(defn new-queue-consumer
  [q-name on-message-fn]
  (map->QueueConsumer {:q-name q-name :on-message-fn on-message-fn}))

(defrecord MessageProducer [address session]
  component/Lifecycle

  (start [this]
    (let [{:keys [session] :as resources} (if session
                                            {:session session}
                                            (let [f (client-session-factory)
                                                  s (create-session f)]
                                              (log/infof "MessageProducer created a session since none was supplied for address: %s" address)
                                              (start-session s)
                                              {:session s :session-factory f}))
          producer (create-producer session address)]
      (log/infof "Artemis Producer created for address: %s" address)
      (merge this (assoc resources :producer producer))))

  (stop [this]
    (log/infof "Artemis Producer stopping for address: %s" address)
    (some-> this :producer close)

    (when-let [f (:session-factory this)]
      (some-> this :session stop-session)
      (close f))

    (dissoc this :producer :session :session-factory))

  mq/IMessagePublisher
  (publish [{:keys [producer]} msg]
    (let [client-msg (create-durable-message session)]
      ;; set! the message body on the message as edn encoded string
      (message-body-string client-msg (pr-str msg))
      (send producer client-msg))))

(defn new-message-producer
  "New producer that supports publish from IMessagePublisher.
   address is the 'topic' name in JMS terms."
  ([address]
  (map->MessageProducer {:address address}))
  ([client-session address]
   (map->MessageProducer {:session client-session
                          :address address})))

;; An address can be thought of as a topic.
;; All consumers specify an address and a queue. Messages sent to an address
;; are routed to queues that are bound to that address.
;;
;; Each console will create a temporary queue to the address jsk.director.broadcast
