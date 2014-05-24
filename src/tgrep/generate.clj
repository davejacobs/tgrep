(ns tgrep.generate
  (:require [tgrep.search :as ts]
            [clojure.string :as string]
            [clj-time.core :as clj-time]
            [clj-time.coerce :as coerce]
            [clojure.java.io :refer [writer]])
  (:import java.io.BufferedWriter))

;; Copied from old clojure-contrib
(defn write-lines
  "Writes lines (a seq) to f, separated by newlines. f is opened with
   writer, and automatically closed at the end of the sequence."
  [filename lines]
  (with-open [w (writer filename)]
    (binding [*out* w]
      (doseq [line lines]
        (println line)))))

(def start-time (ts/parse-date "11/Feb/2011:23:55:00.000" ts/date-formatter-1))

(def example "Feb 10 10:59:49 web03 haproxy[1631]: 10.350.42.161:58625 [10/Feb/2011:10:59:49.089] frontend pool3/srv28-5020 0/138/0/19/160 200 488 - - ---- 332/332/13/0/0 0/15 {Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.7) Gecko/20100713 Firefox/3.6.7|www.reddit.com| http://www.reddit.com/r/pics/?count=75&after=t3_fiic6|201.8.487.192|17.86.820.117|} \"POST /api/vote HTTP/1.1\"")

(defn entry-for
  "Generates an entry for the given date (in millis) based on template."
  [template millis]
  (let [date (coerce/from-long millis)
        formatted-1 (ts/unparse-date date ts/date-formatter-1)
        formatted-2 (ts/unparse-date date ts/date-formatter-2)
        subst-1 (string/replace-first template ts/date-re-1 formatted-1)
        subst-2 (string/replace-first subst-1 ts/date-re-2 formatted-2)]
    subst-2))

(defn next-entry
  "Returns the next log entry based on the current entry."
  ([curr-entry] (next-entry curr-entry 1000))
  ([curr-entry millis]
   (let [next-date (ts/inc-date (ts/get-date curr-entry) millis)
         formatted-1 (ts/unparse-date next-date ts/date-formatter-1)
         formatted-2 (ts/unparse-date next-date ts/date-formatter-2)
         subst-1 (string/replace-first curr-entry ts/date-re-1 formatted-1)
         subst-2 (string/replace-first subst-1 ts/date-re-2 formatted-2)]
     subst-2)))

(defn create-file!
  "Writes all entries to logfile via lazy map, to avoid heavy memory
   footprint but still harness the power of functional programming."
  [filename start-date n]
  (let [millis (coerce/to-long start-date)
        entry (entry-for example millis)]
    (write-lines filename 
                 (map #(next-entry entry (* 1000 %)) 
                      (range 0 n)))))
