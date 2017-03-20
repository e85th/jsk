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

(defn element-moved
  [event data]
  ;; data has keys :helper :element :data :event
  ;; here should only care about the dnd-data-nodes which is [s/Str]
  (log/infof "The elements in flight: %s" (tree/dnd-data-nodes data)))

(defn element-dropped
  [event data]
  ;; data has keys :helper :element :data :event
  (let [dnd-data (tree/dnd-data data)
        node-id (first (tree/dnd-data-nodes data))]
    (log/infof "Dropped element is node: %s" node-id)
    (log/infof "dnd-dropped keys: %s" (keys (js->clj data :keywordize-keys true)))
    (def ddnd data)
    (log/infof "dnd-dropped obj: %s" (:obj dnd-data))))
