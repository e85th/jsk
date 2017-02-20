(ns jsk.routes
  "Defines routes and helper functions. This namespace needs to be included during app startup
   so that pushy can be initialized."
  (:require [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [jsk.events :as e]
            [jsk.data.explorer.events :as explorer-events]
            [clojure.string :as str]))

(def routes
  ["/jsk/" {"" :jsk/home
            "explorer/" {"" :jsk/explorer}
            "agents/" {"" :jsk.explorer/agent-list
                       [:id "/"] :jsk.explorer/agent}
            "jobs/" {"" :jsk.explorer/job-list
                       [:id "/"] :jsk.explorer/job}
            "schedules/" {"" :jsk.explorer/schedule-list
                          [:id "/"] :jsk.explorer/schedule}}])


(def parse-url* (partial bidi/match-route routes))

(defn- parse-url
  [url]
  (log/infof "input url: %s" url)
  (or (parse-url* url)
      (parse-url* (str url "/"))
      (parse-url* (str/replace url #"/$" ""))))

(defn- matched-route->event
  [{:keys [handler]}]
  (when handler
    (condp = (namespace handler)
      "jsk.explorer" explorer-events/set-explorer-view
      e/set-main-view)))

(defn- dispatch-route
  [matched-route]
  (log/infof "matched route: %s" matched-route)
  (let [le-event (matched-route->event matched-route)]
    (log/infof "le-event: %s" le-event)
    (rf/dispatch [le-event matched-route])))

(def url-for (partial bidi/path-for routes))

(pushy/start! (pushy/pushy dispatch-route parse-url))
