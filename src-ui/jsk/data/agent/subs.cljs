(ns jsk.data.agent.subs
  (:require [re-frame.core :as rf]
            [e85th.ui.rf.macros :refer-macros [defsub]]
            [jsk.data.agent.models :as m]))

(defsub busy? m/busy?)
(defsub current m/current)
(defsub current-name m/current-name)
