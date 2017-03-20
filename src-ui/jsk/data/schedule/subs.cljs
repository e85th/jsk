(ns jsk.data.schedule.subs
  (:require [re-frame.core :as rf]
            [e85th.ui.rf.sweet :refer-macros [def-sub-db]]
            [jsk.data.schedule.models :as m]))

(def-sub-db busy? m/busy?)
(def-sub-db current m/current)
(def-sub-db current-name m/current-name)
(def-sub-db current-desc m/current-desc)
(def-sub-db current-cron-expr m/current-cron-expr)
