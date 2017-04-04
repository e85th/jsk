(ns jsk.data.explorer.subs
  (:require [re-frame.core :as rf]
            [e85th.ui.rf.macros :refer-macros [defsub]]
            [jsk.data.explorer.models :as m]))

(defsub explorer-view m/explorer-view)
