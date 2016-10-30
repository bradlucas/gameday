(ns gameday.core
  (:gen-class)
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [clj-time.core :as time]
            [gameday.gameday :as gd]
            [gameday.download :as download]))

;; schedule DATE
;; Return a list of games for the specific DATE
;;
;; game DATE GAME_ID
;; Show game results for a specific GAME_ID

(def cli-options
  [["-s" "--schedule"]
   ["-g" "--game"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> [""
        "Usage: gameday [option] ARGS"
        ""
        "Options:"
        options-summary
        "-s MM DD YYYY"
        "-g MM DD YYYY NUM"]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main 
  [& args]
  (let [{:keys [options arguments summary errors]} (parse-opts args cli-options)]
    (cond
     (:schedule options) (gd/print-schedule-by-date (time/date-time (Integer. (nth arguments 2)) (Integer. (nth arguments 0)) (Integer. (nth arguments 1))))
     (:game options) (gd/print-game (time/date-time (Integer. (nth arguments 2)) (Integer. (nth arguments 0)) (Integer. (nth arguments 1))) (Integer. (nth arguments 3)))
     (:help options) (exit 0 (usage summary))
     errors (exit 1 (error-msg errors))
     true (exit 0 (usage summary)))))
