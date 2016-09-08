(ns jsk.common.pg
  (:require [clojure.java.jdbc :as jdbc]
            [cheshire.core :as json])
  (:import [org.postgresql.util PGobject]))

(defn as-json-pg-obj
  [x]
  (doto (PGobject.)
    (.setType "json")
    (.setValue (json/generate-string x))))

(extend-protocol jdbc/ISQLValue
  clojure.lang.IPersistentMap
  (sql-value [x] (as-json-pg-obj x))

  clojure.lang.IPersistentVector
  (sql-value [x] (as-json-pg-obj x))

  clojure.lang.IPersistentList
  (sql-value [x] (as-json-pg-obj x)))

(extend-protocol jdbc/IResultSetReadColumn
  PGobject
  (result-set-read-column [pgobj metadata idx]
    (let [v (.getValue pgobj)]
      (case (.getType pgobj)
        "json" (json/parse-string v true)
        "jsonb" (json/parse-string v true)
        :else v))))
