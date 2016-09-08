(defproject e85th/jsk "0.1.0-SNAPSHOT"
  :description "Job Scheduling Kit"
  :url "http://github.com/e85th/jsk"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [e85th/commons "0.1.0-SNAPSHOT"]
                 [e85th/backend "0.1.0-SNAPSHOT"]
                 [org.quartz-scheduler/quartz "2.2.3"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.postgresql/postgresql "9.4.1208"]
                 [prismatic/schema "1.1.2"]
                 [com.taoensso/timbre "4.6.0"]
                 [http-kit "2.2.0"]
                 [metosin/compojure-api "1.1.4"]
                 [metosin/ring-http-response "0.8.0"]
                 [ring/ring-json "0.4.0"]
                 [aero "1.0.0"]
                 ;; required to control logging from java libs
                 [org.slf4j/slf4j-log4j12 "1.7.21"]]


  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]

  ;; quell multiple binding messages from java logging frameworks
  :exclusions [ch.qos.logback/logback-classic
               ch.qos.logback/logback-core]

  :main jsk.main
  :aot :all

  :version-script "git describe --tags || date | md5 || date | md5sum"

  :test-selectors {:default (complement :integration)
                   :integration :integration
                   :all (constantly true)}

  :plugins [[com.jakemccrary/lein-test-refresh "0.10.0"]
            [codox "0.8.13"]
            [lein-exec "0.3.6"]
            [lein-version-script "0.1.0"]
            [test2junit "1.1.2"]
            [lein-kibit "0.1.2"] ; static code analyzer for clojure
            [lein-ancient "0.6.7" :exclusions [org.clojure/clojure]]]

  :profiles {:dev  [:project/dev  :profiles/dev]
             :test [:project/test :profiles/test]
             :uberjar {:aot :all}
             :profiles/dev  {}
             :profiles/test {}
             :project/dev   {:dependencies [[reloaded.repl "0.2.2"]
                                            [org.clojure/tools.namespace "0.2.11"]
                                            [org.clojure/tools.nrepl "0.2.12"]
                                            [ring/ring-mock "0.3.0"]]
                             :source-paths   ["dev/src"]
                             :resource-paths ["dev/resources"]
                             :repl-options {:init-ns user}
                             :env {:port "7000"}}
             :project/test  {:dependencies [[ring/ring-mock "0.3.0"]]}}

  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]

  :codox {:sources ["src/clj"]
          :defaults {:doc "FIXME: write docs"}
          :src-dir-uri "http://github.com/e85th/jsk/blob/master/"
          :src-linenum-anchor-prefix "L"})
