(ns gameday.gameday
  "The core namespace for operations in thr clj-gameday library.
  Download data files from gameday server.
  Import data into local database"
  (:require [clojure.string :as str]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as zf]
            [clj-time.core :as time]))

(defn url-root []  "http://gd2.mlb.com/components/game/mlb")  ;; assumption that directories DONT'T end in slashes

(defn master-scoreboard-filename [] "master_scoreboard.xml")

(defn game-events-filename [] "game_events.xml")

(defn est 
  "The clj-time now function is in UTC. The est function will return EST. 5 hours behind"
  []
  (time/minus (time/now) (time/hours 5)))

(defn edt 
  "Eastern daylight savings time is 4 hours behind UTC"
  []
  (time/minus (time/now) (time/hours 4)))

(defn start-season 
  "The first day of the season is 3/31/2011"
  []
  ;; TODO how to know the beginning of this season
  ;; TODO for now put it in a config file
  ;; (date-time 2011 3 31)
  (time/date-time 2016 04 03))

(defn day-after-end-season 
  "The last day of the season is 9/28/2011. When we calculate if a day
is in the season we need to check if it is less than the day after the
last day."
  []
  ;;(plus (date-time 2011 9 28) (days 1))

  ;; @see https://en.wikipedia.org/wiki/2016_Major_League_Baseball_season
  (time/plus (time/date-time 2016 10 2) (time/days 1)))

(defn in-season 
  "Returns true if the date is in the season. This test is by seeing
if the date is before the day after the last day of the season. We
don't check if the date is before the first day because we are looping
through a lazy-seq which starts with the first day of the season."
  [date]
  (time/before? date (day-after-end-season)))

(defn lazy-dates 
  "Returns a lazy sequence of dates incremented by the increment parameter."
  [start increment]
  (lazy-seq
   (cons start (lazy-dates (time/plus start (time/days increment)) increment))))

(defn season
  "Return a sequence which contains all the days in the season"
  []
  (take-while in-season (lazy-dates (start-season) 1)))

(defn pad 
  "Pads a number to two digits in width by prepending a 0 if
necessary. For example (pad 5) will return '05'."
  [x]
  (if (> 2 (.length (str x)))
    (pad (str 0 x)) 
    (str x)))

(defn format-date 
  "Format a date the manner needed for the MLB directory structure.
year_yyyy/month_mm/day_dd"
  [date]
  (let [dd (pad (time/day date))
	mm (pad (time/month date))
	yyyy (time/year date)]
    (str "year_" yyyy "/" "month_" mm "/" "day_" dd)))

(defn convert-mlb-id-to-gameday-link
  "Convert from id: 2011/08/08/chamlb-balmlb-1
  to gameday_link: 2011_08_08_chamlb_balmlb_1"
  [mlb-id]
  (str/replace (str/replace mlb-id #"/" "_") #"-" "_"))


(defn get-master-scoreboard-url 
  "Concatentate the local-root-path with the formatted date string and
scoreboard filename. This builds local paths that mimic the mlb file
structure. For example,
/var/mbl/datadownload/year_2011/month_07/day_02/master_scoreboard.xml"
  [date]
  (str (url-root) "/" (format-date date) "/" (master-scoreboard-filename)))


(defn get-game-events-url
  "For a given game_id return a url to the games_events.xml file
"
  [date mlb-id]
  (str (url-root) "/"  (format-date date) "/" "gid_" (convert-mlb-id-to-gameday-link mlb-id) "/" (game-events-filename)))


;; Define a local root path to download the xml data files into
;; Call download-today to download the file and return the complete path to the file
;; Use xml/parse to parse and then convert to a zipper with zip/xml-zip
;; To get the first game use zf/xml1-> :game

;; (def xml-file (download-today (mlb-local-storage)))
;; (def zipper (zip/xml-zip (xml/parse xml-file)))
;; (def first-game (zf/xml1-> zipper :game))

;; (declare get-game-maps)
;; (def game-maps (for [x (zf/xml-> zipper :game)]
;; 	      (get-game-maps x)))

(defn get-game-attributes [xml-game]
  (let [g xml-game]
    (hash-map
     :id (zf/attr g :id)
     :time (zf/attr g :time)
     :time-zone (zf/attr g :time_zone)
     :ampm (zf/attr g :ampm)
     :home-team-name (zf/attr g :home_team_name)
     :home-team-city (zf/attr g :home_team_city)
     :home-team-id (zf/attr g :home_team_id)
     :venue (zf/attr g :venue)
     :away-team-name (zf/attr g :away_team_name)
     :away-team-city (zf/attr g :away_team_city)
     :away-team-id (zf/attr g :away_team_id))))
  
(defn get-game-status [xml-game]
  (let [s (zf/xml1-> xml-game :status)]
    (hash-map
     :status (zf/attr s :status)
     :reason (zf/attr s :reason)
     :inning (zf/attr s :inning)
     :top-inning (zf/attr s :top_inning)
     :inning-state (zf/attr s :inning_state)
     )))

(defn get-game-linescore [xml-game]
  (let [linescore (zf/xml1-> xml-game :linescore)]
    (when linescore 
        (let [r (zf/xml1-> linescore :r)]
          (hash-map
           :runs-home (zf/attr r :home)
           :runs-away (zf/attr r :away))))))

(defn parse-games [xml-file]
  (let [xml-parse (xml/parse xml-file)
        ;; attrs (:attrs xml-parse)
        ;; year (:year attrs)
        ;; month (:month attrs)
        ;; day (:day attrs)
        ]
    (let [zipper (zip/xml-zip xml-parse)]
      (zf/xml-> zipper :game))))

(defn get-game-map [xml-game]
  (let [a (get-game-attributes xml-game)
        s (get-game-status xml-game)
        l (get-game-linescore xml-game)]
    (merge l s a)))

(defn get-games
  [date]
  (let [url (get-master-scoreboard-url date)
        games (map #(get-game-map %) (parse-games url))]
    games))

(defn- in-progress-p
  [game-map]
  (= (:status game-map) "In Progress"))

(defn- final-p
  [game-map]
  (= (:status game-map) "Final"))

(defn- get-game-ids
  [games]
  (map #(% :id) games))

(defn get-in-progress-games
  ""
  [date]
  (get-game-ids (filter #(in-progress-p %) (get-games date))))

(defn get-final-games
  ""
  [date]
  (get-game-ids (filter #(final-p %) (get-games date))))

(defn show-game-time
  [game]
  (str (game :away-team-name) " vs " (game :home-team-name) " at " (game :time) " " (game :ampm)))

(defn print-with-index
  [col]
  ;; print as "1 - item"
  (let [cnt (range (count col))]
    (doall (map (fn [cnt row] (println (format "%d - %s" cnt row))) cnt col))))

(defn schedule-by-date
  [date]
  (let [games (get-games date)]
    (map #(show-game-time %) games)))

(defn print-schedule-by-date
  [date]
  (print-with-index (schedule-by-date date)))

(defn print-game-ex
  [date game]
  (do
    (println "mlb_id " (game :id))
          
    (println "month " (pad (time/month date)))
    (println "day " (pad (time/day date)))
    (println "year   " (time/year date))
    
    (println "time " (game :time ))
    (println "time-zone " (game :time-zone))
    (println "ampm " (game :ampm))
    
    ;; (println "week_id" (db/get-week-id date))
    
    (println "home-team-name " (game :home-team-name))
    (println "home-team-city " (game :home-team-city))
    (println "home-team-id " (game :home-team-id))
    (println "venue " (game :venue))
    (println "away-team-name " (game :away-team-name))
    (println "away-team-city " (game :away-team-city))
    (println "away-team-id " (game :away-team-id))
    
    (println "top-inning " (game :top-inning))
    (println "reason " (game :reason))
    (println "inning " (game :inning))
    (println "inning-state " (game :inning-state))
    (println "status " (game :status))
    (println "runs-home " (game :runs-home))
    (println "runs-away " (game :runs-away))))

(defn show-game
  [date idx]
  ;; get the scorecard for the day
  ;; get the game by id (index)
  (let [games (get-games date)]
    (if games
      (let [game (nth games idx)]
        ;; (print-game-ex date game)
        game)
      (println "No games found for %s" date)
      )))

(defn print-game
  [date idx]
  (print-game-ex date (show-game date idx)))
