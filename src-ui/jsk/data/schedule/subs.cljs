(ns jsk.data.schedule.subs
  (:require [re-frame.core :as rf]
            [e85th.ui.rf.macros :refer-macros [defsub]]
            [jsk.data.schedule.models :as m]))

(defsub busy? m/busy?)
(defsub current m/current)
(defsub current-name m/current-name)
(defsub current-desc m/current-desc)
(defsub current-cron-expr m/current-cron-expr)
