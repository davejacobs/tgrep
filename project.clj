(defproject tgrep "0.0.1"
  :description "A speedy log parser"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [clj-time "0.7.0"]]
  :aliases {"search" ["run" "-m" "tasks.search"]
            "generate" ["run" "-m" "tasks.generate"]})
