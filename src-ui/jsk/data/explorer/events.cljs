(ns jsk.data.explorer.events
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [jsk.common.data :as data]
            [jsk.net.api :as api]
            [jsk.websockets :as websockets]
            [jsk.data.explorer.models :as m]
            [e85th.ui.rf.sweet :refer-macros [def-event-db def-event-fx def-db-change]]
            [e85th.ui.util :as u]
            [re-frame.core :as rf]))

(def-db-change set-explorer-view m/explorer-view)
