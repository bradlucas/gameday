(ns gameday.download
  "Routines for downloading file data from the mlb gameday server"
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-time.core :as time]
            [gameday.gameday :as gd]))

;; TODO this should be configurable. ENV 
(defn mlb-local-storage
  "Return the storage path for the machine we are running on.
Do not have a slash at the end
"
  []
  (let [hostname (.. java.net.InetAddress getLocalHost getHostName)]
    (cond
     (= hostname "bwl.local") "/Users/brad/www/fannedout-dev/files/mlb"
     (= hostname "beaconhill.com") "/home/blucas/www/fannedout-test/files/mlb"
     true (System/getProperty "user.dir"))))

(defn get-master-scoreboard-path 
  "Concatentate the local-root-path with the formatted date string and
scoreboard filename. This builds local paths that mimic the mlb file
structure. For example,
/var/mbl/datadownload/year_2011/month_07/day_02/master_scoreboard.xml"
  [local-root-path date]
  (str local-root-path "/" (gd/format-date date) "/" (gd/master-scoreboard-filename)))

(defn get-file 
  "Download via HTTP remote_url and save it locally into
local-file. Take precaution that you can write the local-file by
calling make-parents to create the directory structure local-file is
in."
  [remote-url local-file]
  (do
    (io/make-parents local-file)
    (spit local-file (slurp remote-url))
    local-file))

(defn download-file [storage-path date]
  (let [local-file (get-master-scoreboard-path storage-path date)
        remote-url (gd/get-master-scoreboard-url date)]
    (get-file remote-url local-file)))

(defn download-today 
  "Load today's data file by building the date and creating the url o the scoreboard file"
  [storage-path]
  (download-file (mlb-local-storage) (gd/edt)))

(defn download-season
  "Download all the scores for the entire season"
  []
  (map #(download-file (mlb-local-storage) %) (gd/season)))
  ;; (let [season (take-while in-season (lazy-dates (start-season) 1))]
  ;;   (map #(download-file (mlb-local-storage) %) (season))))

(defn download-scores-file
  "Public function to download the master_scoreboard.xml file for a given date"
  [date]
  (download-file (mlb-local-storage) date))

;; Download master_scoreboard.xml file
(defn download-scores
  "Download score data from mlb server"
  ([] (download-scores-file (gd/est)))
  ([date] (download-scores-file date))
  ([m d y] (download-scores-file (time/date-time y m d))))

(defn download-scores-season
  "Download the entire season's scores"
  []
  (download-season))

