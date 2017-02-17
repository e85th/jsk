(ns jsk.common.util
  (:require [e85th.commons.util :as u]))

(def group-id "e85th")
(def artifact-id "jsk")
(def build-properties (partial u/build-properties group-id artifact-id))
(def build-properties-with-header (partial u/build-properties-with-header group-id artifact-id))
(def build-version (partial u/build-version group-id artifact-id))
