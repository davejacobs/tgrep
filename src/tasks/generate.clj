(ns tasks.generate
  (:require [tgrep.generate :as generate]
            [clojure.tools.cli :as cli]
            [clojure.string :as string])
  (:gen-class))

(def cli-options
  [["-f" "--file FILE" 
    "File to generate"
    :default "haproxy.log"]
   ["-s" "--start TIME" 
    "Start time for start of log filel"
    :default "00:00"]
   ["-n" "--number NUMBER" 
    "Number of lines to generate"
    :parse-fn #(Integer/parseInt %)
    :default 1000000]
   ["-v" "--verbose" "Verbose"]])

(defn -main [& args]
  (let [{:keys [options]} (cli/parse-opts args cli-options)
        {:keys [file start number]} options]
    (generate/create-file!  file start number)))
