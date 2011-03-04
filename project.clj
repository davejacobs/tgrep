(defproject tgrep "0.0.1"
  :description "FIXME: write"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [clj-time "0.3.0-SNAPSHOT"]]
  :run-aliases {:tgrep tgrep.search
                :generate tgrep.generate}
  :main tgrep.search)
