(ns jsk.login.subs
  (:require [re-frame.core :as rf]
            [e85th.ui.rf.macros :refer-macros [defsub]]
            [jsk.login.models :as m]))

(defsub email m/email)
(defsub password m/password)
(defsub login-busy? m/login-busy?)
