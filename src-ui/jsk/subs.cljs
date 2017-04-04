(ns jsk.subs
  (:require [re-frame.core :as rf]
            [e85th.ui.rf.macros :refer-macros [defsub]]
            [taoensso.timbre :as log]
            [jsk.models :as m]))

(defsub main-view m/main-view)
(defsub current-user m/current-user)
