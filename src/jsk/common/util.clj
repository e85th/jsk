(ns jsk.common.util
  (:require [e85th.commons.util :as util]))

(def group-id "e85th")
(def artifact-id "jsk")
(def build-properties (partial util/build-properties group-id artifact-id))
(def build-properties-with-header (partial util/build-properties-with-header group-id artifact-id))
