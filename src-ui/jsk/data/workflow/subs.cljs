(ns jsk.data.workflow.subs
  (:require [re-frame.core :as rf]
            [e85th.ui.rf.macros :refer-macros [defsub defsub-db]]
            [taoensso.timbre :as log]
            [jsk.data.workflow.models :as m]))

(defsub current-alerts m/current-alerts)
(defsub current-desc m/current-desc)
(defsub current-enabled? m/current-enabled?)
(defsub current-name m/current-name)
(defsub current-schedules m/current-schedules)
(defsub current-tags m/current-tags)

(defsub busy? m/busy?)
