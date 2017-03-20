(ns jsk.data.alert.subs
  (:require [re-frame.core :as rf]
            [e85th.ui.rf.sweet :refer-macros [def-sub-db def-sub]]
            [jsk.data.alert.models :as m]))

(def-sub-db busy? m/busy?)
(def-sub-db current m/current)
(def-sub-db current-name m/current-name)
(def-sub-db current-desc m/current-desc)
(def-sub-db current-channels m/current-channels)

(def-sub-db selected-channel m/selected-channel)
(def-sub-db channel-suggestions m/channel-suggestions)
