(defproject org.clojars.simon_brooke/real-name "1.0.2"
  :codox {:metadata {:doc "**TODO**: write docs"
                     :doc/format :markdown}
          :output-path "docs"
          :source-uri "https://github.com/simon-brooke/real-name/blob/master/{filepath}#L{line}"}
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :description "Resolve real names from user names in a platform independent fashion."
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :plugins [[lein-codox "0.10.7"]]
  :repl-options {:init-ns cc.journeyman.real-name.core}
  :url "https://github.com/simon-brooke/real-name/")
