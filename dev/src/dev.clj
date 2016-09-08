(ns dev
  (:refer-clojure :exclude [test])
  (:require [clojure.repl :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [jsk.main :as main]
            [reloaded.repl :refer [system init start stop go reset]]))

(reloaded.repl/set-init! main/make-system)
