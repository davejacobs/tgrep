(ns tgrep.search
  (:require [clj-time.format :only (formatter)])
  (:use [clojure.contrib.string :only (join split)]
        [clj-time.core :exclude (extend)]
        clj-time.coerce)
  (:import java.io.RandomAccessFile))

; Defaults/parameters
(def *logfile* "/logs/haproxy.log")
(def *encoding* "ASCII")
(def *linefeed* "\n")
(def *line-re*
  (re-pattern (str *linefeed* "([^" *linefeed* "]+)" *linefeed*)))
(def *entry-len* 300) ; Default expected entry length

; Date formats and matchers
; Fully qualified format
(def date-formatter-1 
  (clj-time.format/formatter "dd/MMM/yyyy:HH:mm:ss.SSS"))

; Qualified format witHout year
(def date-formatter-2
  (clj-time.format/formatter "MMM dd HH:mm:ss"))

; Qualified format witH year
(def date-formatter-3
  (clj-time.format/formatter "MMM dd yyyy HH:mm:ss"))

; Date mask with year
(def date-formatter-4
  (clj-time.format/formatter "MMM dd yyyy"))

; Date mask witHout year
(def date-formatter-5
  (clj-time.format/formatter "MMM dd"))

; Time mask witHout milliseconds
(def date-formatter-6
  (clj-time.format/formatter "HH:mm:ss"))

; Fully qualified regular expression
(def date-re-1 #"\d{2}/\w{3}/\d{4}:\d{2}:\d{2}:\d{2}\.\d{3}")

; Qualified regular expression without year
(def date-re-2 #"^\w{3}\s{1,2}\d{2}\s\d{2}:\d{2}:\d{2}")

; Date manipulators
(defn parse-date
  "Parses time from string according to date formatter"
  [string formatter] (clj-time.format/parse formatter string))
  
(defn unparse-date
  "Emits date according to date formatter"
  [date formatter] (clj-time.format/unparse formatter date))

(defn get-date
  "Matches date according to re, returning a parseable substring 
  containing the date"
  ([string] (get-date string date-re-2))
  ([string re] 
   (let [match (re-find re string)
         date (if match (parse-date match date-formatter-2) nil)]
     date)))

(defn inc-date
  "Create date that is x milliseconds past this date"
  ([date] (inc-date date 1))
  ([date millis] (.plusMillis date millis)))

; Predicates for file search functions
(defn- line? 
  "Does s contain a full line?"
  [s] (re-find *line-re* s))

(defn- not-line?
  "Does s not contain a full line?"
  [s] (not (line? s)))

(defn- not-before?
  "Is the current date after or equal to the start of interval? If
  current is not parseable, will also return true."
  [interval current]
  (let [match (re-find date-re-2 current)
        start (start interval)]
    (if-not match
      true
      (or (after? (get-date match) start)
          (.equals start (get-date match))))))

(defn- not-after?
  "Is the current date before or equal to the end of interval? If
  current is not parseable, will also return true."
  [interval current]
  (let [match (re-find date-re-2 current)
        end (end interval)]
    (if-not match
      true
      (or (before? (get-date match) end)
          (.equals end (get-date match))))))

; Line parser
(defn get-line 
  "Gets the first full line in s."
  [s] (second (re-find *line-re* s)))

; File search tools
 
(defn file-contents-at
  "Quick random access lookup of file contents from start to start + len."
  [file start]
  (let [b (byte-array *entry-len*)]
    (.seek file start)
    (.read file b)
    (String. b *encoding*)))

(defn file-seek-backwards-while
  "Seeks backwards as long as the predicate is met. Will return the
  aggregate of all chunks where the predicate is met, including the 
  first one where the predicate is not met. This allows
  for finding the beginning of lines."
  ([file start predicate] 
   (file-seek-backwards-while file start predicate *entry-len*))
  ([file start predicate buffer-len]
   (def b (byte-array buffer-len))
   (loop [position start
          aggregate ""]
     (let [safe-position (if (neg? position) 0 position)
           characters (file-contents-at file safe-position)
           next-pos (- safe-position buffer-len)
           next-agg (str characters aggregate)
           still-seeking? (predicate next-agg)]
       (if (or (= safe-position 0) (not still-seeking?))
         next-agg
         (recur next-pos next-agg))))))

(defn file-line-around
  "Returns the line containing the byte indicated by start."
  [file start]
  (.seek file start)
  (let [end-of-line (.readLine file)
        beginning-of-line 
          (file-seek-backwards-while file start not-line?)
        full-string (str beginning-of-line end-of-line)
        full-line (get-line full-string)]
    full-line))

(defn file-prev-lines-while
  "Returns the aggregate of all lines before the line containing start, 
  as long as the predicate is met. Unline file-seek-backwards-while, this
  does not include the first line that doesn't meet the predicate 
  requirements."
  [file start predicate]
  (.seek file start)
  (loop [position (- start *entry-len*)
         aggregate ""]
    (let [safe-position (if (neg? position) 0 position)
          line (file-seek-backwards-while file safe-position not-line?)
          match (get-line line)
          next-pos (- safe-position *entry-len*)]
      (if-not (predicate match)
        aggregate
        (recur next-pos (str match "\n" aggregate))))))

(defn file-next-lines-while
  "Returns the lines following the current line until predicate is
  not met. Will not return any lines that do not meet predicate."
  [file start predicate]
  (.seek file start)
  (.readLine file) ; Skip rest of the current line
  (loop [aggregate ""
         line (.readLine file)]
    (if-not (predicate line)
      aggregate
      (recur (str aggregate "\n" line) (.readLine file)))))

(defn- correct-bound
  "Determines whether, when we're looking for target and have
  found date, we should look higher or lower"
  [target date lower upper]
  ;(println "finding correct bound for" target date lower upper)
  (cond (after? target date) upper
        (before? target date) lower
        :else nil))

(defn find-time-index
  "Binary search algorithm to quickly find the position of the indicated
  target in the file."
  ([file target] (find-time-index file target 0 (.length file)))
  ([file target lower-bound upper-bound]
   (loop [lower lower-bound
          upper upper-bound]
     (let [position (quot (+ lower upper) 2)
           guess (file-line-around file position)
           date (get-date guess)
           bound (correct-bound target date lower upper)]
       (if (nil? bound)
         position
         (recur (min position bound) 
                (max position bound)))))))

(defn find-lines
  "Returns all lines (as a sequence) whose dates fall inside of
  interval."
  [file interval]
  (let [init (find-time-index file (start interval))
        first-pred #(not-before? interval %)
        first-segment (file-prev-lines-while file init first-pred) 
        middle-segment (file-line-around file init)
        last-pred #(not-after? interval %)
        last-segment (file-next-lines-while file init last-pred)
        full-segment (str first-segment middle-segment last-segment)
        lines (split #"\n" full-segment)]
    lines))

; Normalizing functions
(defn combine-date-time
  "Combine month/day/year from date with hour/minute/second
  from rel-time"
  [date time-string]
  (let [date-mask date-formatter-4
        combined-mask date-formatter-3
        date-string (unparse-date date date-mask)
        combined (join " " [date-string time-string])
        parsed (parse-date combined combined-mask)]
    parsed))

(defn intervalize
  "Returns an interval represented by the time-string in the context
  of date. An interval gives the precise start and stop times for
  a period of time down to the second, where time string may be 
  arbitrarily vague. For example, 01:00 would specify anything in 
  between 1:00:00 and 1:00:59. Given two dates and time-strings, 
  will return the interval encompassing both times within their
  own date context."
  ([date time-string]
   (let [[h m s] (split #":" time-string)
         lower-h (or h "00")
         lower-m (or m "00")
         lower-s (or s "00")
         higher-h (or h "23")
         higher-m (or m "59")
         higher-s (or s "59")
         lower (join ":" [lower-h lower-m lower-s])
         higher (join ":" [higher-h higher-m higher-s])]
     (interval (combine-date-time date lower)
               (combine-date-time date higher))))
  ([date-1 time-string-1 date-2 time-string-2]
   (let [interval-1 (intervalize date-1 time-string-1)
         interval-2 (intervalize date-2 time-string-2)
         time-1 (start interval-1)
         time-2 (end interval-2)]
     (interval time-1 time-2))))

(defn within-interval?
  "Is inside-interval fully inside of outside-interval?"
  [outside-interval inside-interval]
  (let [start (start inside-interval)
        end (end inside-interval)]
    (and (within? outside-interval start)
         (within? outside-interval end))))

(defn time-interval-within-interval
  "Given an interval slightly greater than 24 h and a relative time,
  will give the first absolute date with the relative time within
  that interval."
  [interval time-string]
  (let [interval-1 (intervalize (start interval) time-string)
        interval-2 (intervalize (end interval) time-string)]
    (cond (within-interval? interval interval-1) interval-1
          (within-interval? interval interval-2) interval-2
          :else nil)))

(defn process-file
  "Opens up a file to search for all lines whose dates are between
  start-time and end-time, inclusive. Prints those results line-by-line."
  [filename start-time end-time]
   (with-open [file (RandomAccessFile. filename "r")]
     (let [first-entry (.readLine file)
           last-entry (file-line-around file (dec (.length file)))
           log-start (get-date first-entry)
           log-end (get-date last-entry)
           log-interval (interval log-start log-end)
           start-interval (time-interval-within-interval log-interval start-time)
           end-interval (time-interval-within-interval log-interval end-time)
           target-interval (interval (start start-interval) (end end-interval))
           lines (find-lines file target-interval)]
       (doseq [line lines]
         (println line)
         (println "-----")))))

(defn -main
  ([filename start-time] (-main filename start-time start-time))
  ([filename start-time end-time]
   (time (process-file filename start-time end-time))))
