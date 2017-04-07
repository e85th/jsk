(ns jsk.data.workflow.graph
  (:require [schema.core :as s]))

(s/defschema Node
  {:node/id s/Str ; id for the dom and db
   :node/name s/Str ; job or workflow name
   :node/type s/Keyword ; :job or :workflow
   :node/referent   s/Int ;; job or wf id
   :node/successors  #{s/Str}
   :node/successors-err #{s/Str}
   :node/style s/Str})

(s/defschema Graph
  ;; the string key is the dom-id for either the job or workflow
  {s/Str Node})

(s/defn node :- Node
  "Creates a node."
  ([id name type referent]
   (node id name type referent #{} #{} ""))
  ([id name type referent successors successors-err style]
   {:node/id id
    :node/name name
    :node/type type
    :node/referent referent
    :node/successors successors
    :node/successors-err successors-err
    :node/style style}))

(s/defn add-node :- Graph
  [g :- Graph node :- Node]
  (assoc g (:node/id node) node))

(defn dom-id->db-id
  [graph dom-id]
  (get-in graph [dom-id :id]))

(defn- update-edge
  "f is the operation conj/disj. kind is successors or successors-err"
  [f successor-kind graph source-dom-id target-id]
  (update-in graph [source-dom-id successor-kind] f target-id))

(def add-successor (partial update-edge conj :node/successors))
(def add-successor-err (partial update-edge conj :node/successors-err))
(def rm-successor (partial update-edge disj :node/successors))
(def rm-successor-err (partial update-edge disj :node/successors-err))

(defn node-count
  [g]
  (count g))

(defn nodes
  "Get all the nodes in the graph."
  [g]
  (vals g))


(defn node-dom-id
  "Generates the workflow dom-id"
  []
  (str (gensym "wf-node-")))

(defn new-graph
  []
  {})
