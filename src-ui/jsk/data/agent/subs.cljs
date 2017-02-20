(ns jsk.data.agent.subs
  (:require [re-frame.core :as rf]
            [e85th.ui.rf.sweet :refer-macros [def-sub-db]]
            [jsk.data.agent.models :as m]))

(def-sub-db agent-list m/agent-list)
(def-sub-db busy? m/busy?)
(def-sub-db current m/current)
(def-sub-db current-name m/current-name)
