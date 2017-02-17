(ns jsk.login.subs
  (:require [re-frame.core :as rf]
            [e85th.ui.rf.sweet :refer-macros [def-sub-db]]
            [jsk.login.models :as m]))

(def-sub-db email m/email)
(def-sub-db password m/password)
(def-sub-db login-busy? m/login-busy?)
