(ns jsk.data.explorer.subs
  (:require [re-frame.core :as rf]
            [e85th.ui.rf.sweet :refer-macros [def-sub-db]]
            [jsk.data.explorer.models :as m]))

(def-sub-db explorer-view m/explorer-view)
