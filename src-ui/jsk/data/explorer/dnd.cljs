(ns jsk.data.explorer.dnd
  (:require [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.rf.tree :as tree]
            [jsk.data.explorer.events :as e]
            [jsk.data.explorer.models :as m]))

(def draggable-types #{:workflow :job :schedule :alert})

(defn draggable?
  [nodes]
  ;; NB. Assuming nodes are only draggable one at a time
  (->> (aget nodes 0) m/explorer-node :type draggable-types some?))

(defn started
  "Records the tree node that started being dragged in app-db to be used by
   listeners of the drop event."
  [event data]
  ;; data has keys :helper :element :data :event
  ;; here should only care about the dnd-data-nodes which is [s/Str]
  (let [node-id (first (tree/dnd-data-nodes data))]
    (rf/dispatch [e/set-dnd-node (tree/original-node (tree/node m/tree-sel node-id))])))
