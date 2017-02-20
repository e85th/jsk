(ns jsk.data.job-types
  "Need to make this functiona via a plugin system. Hardcoded for now."
  (:require [schema.core :as s]
            [jsk.data.models :as m]))

(def job-type->schema
  {:shell [{:job.field/name "Execution Directory"
            :job.field/desc "The directory the job runs from."
            :job.field/key :shell/exec-dir
            :job.field/type :string
            :job.field/required? true}
           {:job.field/name "Command Line"
            :job.field/desc "The command to run."
            :job.field/key :shell/cmd-line
            :job.field/type :string
            :job.field/required? true}]})


(s/defn find-all :- m/JobTypeSchema
  "Finds all job types and associated schemas"
  [res]
  job-type->schema)
