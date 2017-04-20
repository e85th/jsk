(ns jsk.director.models
  (:require [schema.core :as s]))

(s/defschema NodeSchedules
  {s/Int #{s/Str}})
