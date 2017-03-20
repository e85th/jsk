(ns jsk.data.workflow.subs
  (:require [re-frame.core :as rf]
            [e85th.ui.rf.sweet :refer-macros [def-sub-db def-sub]]
            [taoensso.timbre :as log]
            [jsk.data.workflow.models :as m]))

(def-sub-db current-desc m/current-desc)
(def-sub-db current-enabled? m/current-enabled?)
(def-sub-db current-name m/current-name)
(def-sub-db current-schedules m/current-schedules)
(def-sub-db current-tags m/current-tags)

(def-sub-db busy? m/busy?)
