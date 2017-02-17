(ns jsk.subs
  (:require [re-frame.core :as rf]
            [e85th.ui.rf.sweet :refer-macros [def-sub-db]]
            [jsk.models :as m]))

(def-sub-db current-view m/current-view)
(def-sub-db current-user m/current-user)
