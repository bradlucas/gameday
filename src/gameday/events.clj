(ns gameday.events
  "Routines for processing game events (game_events.xml) from the GameDay server"
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as zf]
            [gameday.gameday :as gd]))

(defn game-events-file
  "For a given game_id return the contents of the game's game_events.xml file"
  [date mlb-id]
  (let [url (gd/get-game-events-url date mlb-id)]
    (slurp url)))

(defn game-comments-all
  [date mlb-id]
  (let [url (gd/get-game-events-url date mlb-id)]
    (for [x (xml-seq (xml/parse url))
          :when (or (= :atbat (:tag x))
                    (= :action (:tag x)))]
      (let [a (:attrs x)
            tag (:tag x)
            rtn {:comment (:des a)}]
        (if (= :atbat tag)
          (assoc rtn :time (:start_tfs_zulu a))
          (assoc rtn :time (:tfs_zulu a)))))))

(defn game-comments-latest
  [date mlb-id]
  (last (game-comments-all date mlb-id)))

(defn- update-game-banter-helper
  [game-id event]
  (if event
    (let [date-time (event :time)
          comment (event :comment)]
      (if (< 0  (count date-time))
        (do 
          (print event)
          ;; (update-banter-table game-id date-time comment)
          )))))

;; (defn update-game-banter-latest
;;   "Given the latest banter insert/update the comment into the game table.

;; This function looks for the latest comment."
;;   [date mlb-id]
;;   (let [event (game-comments-latest date mlb-id)
;;         game-id (get-game-id mlb-id)]
;;     (update-game-banter-helper game-id event)))

;; (defn update-game-banter-all
;;   "For a given game-id update all the banter.

;; This function updates all the comments for a game."
;;   [date mlb-id]
;;   (let [events (game-comments-all date mlb-id)
;;         game-id (get-game-id mlb-id)]
;;     (doseq [event events]
;;       (do
;;         (print "Event: " (event :comment))
;;         (update-game-banter-helper game-id event)))))


;; These are for testing...
(defn sleep
  "Go to sleep for seconds"
  [seconds]
  (Thread/sleep (* seconds 1000)))

;; Accept count and call process-game-events for that number with a 15 second delay in-between
;; (defn process-events-for-a-while
;;   [total-count count]
;;   (if (= count total-count)
;;     nil
;;     (do
;;       (print "Processing game events\n")
;;       (process-game-events)
;;       (sleep 15)
;;       (recur total-count (+ 1 count)))))

;; (defn- import-events-helper
;;   [date game-ids]
;;   (doseq [game-id game-ids]
;;     (do
;;       (print "Game: " game-id "\n")
;;       (update-game-banter-all date game-id))))
  
;; (defn import-final-events
;;   "For a given date get all the day's game events and load them into the system
;; mm dd yyyy as strings (for now)
;; "
;;   [date]
;;   (import-events-helper date (get-final-games date)))

;; ;; Process the games_events.xml file for game 'In Progress' 
;; (defn import-in-progress-events
;;   "Process the current game_events"
;;   [date]
;;   (import-events-helper date (get-in-progress-games date)))



  
(defn foo
  [m]
  (count (filter #(= (:tag %) :inning) (:content m))))

(defn get-innings [xml]
  (filter #(= :inning (:tag %)) xml))

(defn process-inning-half [h]
  (let [tb (:tag h)
        events (:content h)]
    (doseq [event events]
      (when (or (= :atbat (:tag event))
                 (= :action (:tag event)))
        (print "Inning: " tb " " (:tag event) "\n" )))))

(defn process-inning [inning]
  (doseq [half (:content inning)]
    (process-inning-half half)))

(defn process-innings [innings]
  (map #(process-inning %) innings))

;; (defn game-comments-all2
;;   [date mlb-id]
;;   (let [url (get-game-events-url date mlb-id)
;;         xml (xml-seq (xml/parse url))
;;         innings (get-innings xml)]
;;     (process-innings innings)))
    
