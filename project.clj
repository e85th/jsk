(defproject e85th/jsk "0.1.0-SNAPSHOT"
  :description "Job Scheduling Kit"
  :url "http://github.com/e85th/jsk"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.473"]
                 [org.clojure/tools.cli "0.3.5"]
                 [com.datomic/datomic-free "0.9.5554"]

                 ;; https://github.com/ptaoussanis/encore/blob/master/DEP-CONFLICT.md
                 [com.taoensso/sente "1.11.0"] ; websockets, keep above e85th commons because of dependency issue with encore
                 [com.taoensso/timbre "4.7.4"]
                 [e85th/commons "0.1.12"]
                 [e85th/backend "0.1.20" :exclusions [[com.google.guava/guava-jdk5]]]
                 [e85th/ui "0.1.17"]
                 [org.quartz-scheduler/quartz "2.2.3"]
                 [clojurewerkz/quartzite "2.0.0"]    ; clojure wrapper around quartz scheduling
                 [prismatic/schema "1.1.4"]
                 [com.taoensso/timbre "4.7.4"]
                 [http-kit "2.2.0"]
                 [com.cemerick/url "0.1.1"]
                 [hiccup "1.0.5"]
                 [metosin/compojure-api "1.1.4"]
                 [metosin/ring-http-response "0.8.0"]
                 [aero "1.0.0"]

                 [buddy "1.0.0"]

                 [enlive "1.1.6"] ; html transforms
                 [org.jsoup/jsoup "1.8.3"] ; for use with enlive
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [re-frame "0.8.0"]
                 [day8.re-frame/http-fx "0.0.4"]
                 [reagent "0.6.0"]
                 [cljsjs/react-dom "15.3.1-0"]
                 [cljsjs/firebase "3.2.1-0"]
                 [cljsjs/google-platformjs-extern "1.0.0-0"]
                 [cljsjs/typeahead-bundle "0.11.1-2"]
                 [funcool/hodgepodge "0.1.4"] ;; local storage
                 [kioo "0.4.2"]
                 [devcards "0.2.1-7"] ;; for development
                 [kibu/pushy "0.3.6"] ;; clean url using html5 pushState
                 [bidi "2.0.16"] ;; client site routing
                 [cljs-ajax "0.5.8"]
                 [hipo "0.5.2"] ;; hiccup -> dom modifiable item (for working with other js libs)
                 ;; required to control logging from java libs
                 [org.slf4j/slf4j-log4j12 "1.7.21"]]


  :source-paths ["src"]

  ;; quell multiple binding messages from java logging frameworks
  :exclusions [ch.qos.logback/logback-classic
               ch.qos.logback/logback-core]

  :main jsk.main
  :aot :all

  :version-script "git describe --tags || date | md5 || date | md5sum"

  :test-selectors {:default (complement :integration)
                   :integration :integration
                   :all (constantly true)}

  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :jsk-ui :compiler :output-dir]
                                    [:cljsbuild :builds :jsk-ui :compiler :output-to]]

  :cljsbuild {:builds {:jsk-ui {:source-paths ["src-ui" "checkouts/ui/src/cljs"]
                                :figwheel true
                                :compiler {:main jsk.main
                                           :output-to "resources/public/js/jsk-ui.js"
                                           :output-dir "resources/public/js/out/jsk-ui"
                                           :asset-path "/js/out/jsk-ui"
                                           :optimizations :none
                                           :pretty-print true
                                           :parallel-build true
                                           :closure-defines {goog.DEBUG true}}}}}
  :figwheel {:css-dirs ["resources/public/css"]}
  :plugins [[com.jakemccrary/lein-test-refresh "0.10.0"]
            [codox "0.8.13"]
            [lein-cljsbuild "1.1.4"]
            [lein-version-script "0.1.0"]
            [test2junit "1.1.2"]]

  :profiles {:dev  [:project/dev  :profiles/dev]
             :test [:project/test :profiles/test]
             :uberjar {:aot :all
                       :hooks [leiningen.cljsbuild]
                       :cljsbuild {:jar true
                                   :builds {:jsk-ui {:compiler {:optimizations :advanced
                                                                :closure-defines {goog.DEBUG false}
                                                                :pretty-print false}}}}}
             :profiles/dev  {}
             :profiles/test {}
             :project/dev   {:dependencies [[reloaded.repl "0.2.2"]
                                            [org.clojure/tools.namespace "0.2.11"]
                                            [org.clojure/tools.nrepl "0.2.12"]
                                            [re-frisk "0.3.2"]
                                            [e85th/test "0.1.2"]
                                            [expectations "2.2.0-alpha1"]]
                             :source-paths   ["dev/src"]
                             :resource-paths ["dev/resources"]
                             :plugins [[lein-figwheel "0.5.7"]]
                             :repl-options {:init-ns user}
                             :env {:port "7000"}}
             :project/test  {}}

  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]

  :codox {:sources ["src/clj"]
          :defaults {:doc "FIXME: write docs"}
          :src-dir-uri "http://github.com/e85th/jsk/blob/master/"
          :src-linenum-anchor-prefix "L"})
