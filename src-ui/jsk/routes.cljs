(ns jsk.routes
  "Defines routes and helper functions. This namespace needs to be included during app startup
   so that pushy can be initialized."
  (:require [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [jsk.events :as e]
            [clojure.string :as str]))

(def routes
  ["" {"" :jsk/home}
   "/login/" :jsk/login])


(def parse-url* (partial bidi/match-route routes))

(defn- parse-url
  [url]
  (or (parse-url* url)
      (parse-url* (str url "/"))
      (parse-url* (str/replace url #"/$" ""))))

(defn- dispatch-route
  [matched-route]
  (rf/dispatch [e/set-current-view matched-route]))

(def url-for (partial bidi/path-for routes))

(pushy/start! (pushy/pushy dispatch-route parse-url))
