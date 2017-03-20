(ns jsk.data.explorer.models
  (:require [e85th.ui.rf.tree :as tree]))


(def tree-id "jsk-tree")
(def tree-sel (str "#" tree-id))

(def explorer-view [::explorer-view])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Util Fns
(defn explorer-node
  [node]
  (-> (tree/original-node node)
      (update-in [:jsk-node-type] keyword)))
