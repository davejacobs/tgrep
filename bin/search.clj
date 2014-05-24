#!/Users/David/.cljr/bin/jark

(ns tgrep
  (:require [tgrep.search :as ts])
  (:use clojure.contrib.command-line))

(defn invoke
  ([logfile start-time] (invoke logfile start-time start-time))
  ([logfile start-time end-time]
   (time (ts/process-file logfile start-time end-time))))

(defn -main 
  "Search log files in O(log n) in time"
  [& args]
  (println "here")
  (with-command-line args
    [[logfile "log file"]
     [start   "start time, to arbitrary precision"]
     [end     "end time, to arbitrary precision"]]
    (println "start: " start)
    (println "end: " end)
    (println "log file: " logfile)
    (invoke (logfile start end))))
