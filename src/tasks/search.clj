(ns tasks.search
  (:require [tgrep.search :as search]
            [clojure.tools.cli :as cli]
            [clojure.string :as string]))

(def cli-options
  [["-f" "--file FILE" "Logfile to search"
    :default "haproxy.log"]
   ["-v" "--verbose" "Verbose"]])

(defn time-elapsed [start-time-ns end-time-ns]
  (/ (double (- end-time-ns start-time-ns))
     1000000.0))

(defn process-file-and-print-lines [file start-time end-time]
  (let [lines (search/process-file file
                                   start-time 
                                   (or end-time start-time))]
    (doseq [line lines]
      (println line))))

(defn -main [& args]
  (let [{:keys [options arguments]} (cli/parse-opts args cli-options)
        [start-time end-time] (string/split (first arguments) #"[-]")
        script-start-time (System/nanoTime)
        _ (process-file-and-print-lines (options :file) start-time end-time)
        script-end-time (System/nanoTime)
        queried-in (time-elapsed script-start-time script-end-time)]
    (println "Queried in" queried-in "ms")))
