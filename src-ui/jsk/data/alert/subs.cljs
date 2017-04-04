(ns jsk.data.alert.subs
  (:require [re-frame.core :as rf]
            [e85th.ui.rf.macros :refer-macros [defsub defsub-db]]
            [jsk.data.alert.models :as m]))

(defsub busy? m/busy?)
(defsub current m/current)
(defsub current-name m/current-name)
(defsub current-desc m/current-desc)
(defsub current-channels m/current-channels)

(defsub selected-channel m/selected-channel)
(defsub channel-suggestions m/channel-suggestions)
