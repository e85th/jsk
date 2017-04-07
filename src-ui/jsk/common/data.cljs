(ns jsk.common.data
  "Data with local-storage as backing store."
  (:require [schema.core :as s]
            [clojure.string :as str]
            [hodgepodge.core :as hp :refer [local-storage]]))

(s/defn api-host :- (s/maybe s/Str)
  []
  (:api-host local-storage))

(s/defn ws-host
  []
  (some-> (api-host) (str/split "://") second))

(s/defn set-api-host!
  [api-host :- s/Str]
  (assoc! local-storage :api-host api-host))

(s/defn api-host-set? :- s/Bool
  []
  (some? (api-host)))

(s/defn jsk-token :- (s/maybe s/Str)
  []
  (:jsk-token local-storage))

(s/defn set-jsk-token!
  [jsk-token :- s/Str]
  (assoc! local-storage :jsk-token jsk-token))

(s/defn jsk-token-set? :- s/Bool
  []
  (some? (jsk-token)))
