(ns jsk.data.job.props
  (:require [devcards.core :as d :refer-macros [defcard-rg]]
            [kioo.reagent :as k :refer-macros [defsnippet deftemplate]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [e85th.ui.rf.inputs :as inputs]
            [jsk.data.job.events :as e]
            [jsk.data.job.subs :as subs]))

;; (def shell-schema
;;   [{:job.field/name "Execution Directory"
;;     :job.field/desc "The directory the job runs from."
;;     :job.field/key :shell/exec-dir
;;     :job.field/type :string
;;     :job.field/required? true}
;;    {:job.field/name "Command Line"
;;     :job.field/desc "The command to run."
;;     :job.field/key :shell/cmd-line
;;     :job.field/type :string
;;     :job.field/required? true}])

(defn make-control
  [ctrl {field-key :job.field/key}]
  [ctrl [subs/current-props-field-value field-key] [e/current-props-field-value-changed field-key]])

(def field-type->input-control
  {:boolean inputs/checkbox
   :date inputs/date-picker})

(defn field->input-control
  [{field-type :job.field/type
    field-key :job.field/key
    field-name :job.field/name}]
  (let [ctrl (get field-type->input-control field-type inputs/std-text)
        sub [subs/current-props-field-value field-key]
        event [e/current-props-field-value-changed field-key]]
    [:div {:key field-key}
     [:label field-name]
     [ctrl sub event]]))

(defn schema->input-controls
  "Answers with a list of input fields for the fields in props.
   Each control is subscribed to and handles updating the db for the particular props field
   as described by the schema. See jsk.data.job-types on server."
  [schema]
  (map field->input-control schema))
