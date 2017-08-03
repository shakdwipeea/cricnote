(defproject cricnote "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [notifier "0.1.0-SNAPSHOT"]
                 [cheshire "5.7.1"]
                 [clj-http "3.6.1"]
                 [tick "0.2.4"]]
  :main ^:skip-aot cricnote.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
