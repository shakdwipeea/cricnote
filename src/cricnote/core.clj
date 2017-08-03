(ns cricnote.core
  (:gen-class))

(use '[clojure.java.shell :only [sh]])
(require '[clj-http.client :as client])
(require '[cheshire.core :as json])

(require '[tick.core :refer [minutes seconds]]
          '[tick.timeline :refer [timeline periodic-seq]]
          '[tick.clock :refer [now]])

(require '[tick.schedule :as sched])

(def good-teams ["India" "England"])

;;;;;;;;;;;;;;;;;;;;;;
;; CRIC Api helpers ;;
;;;;;;;;;;;;;;;;;;;;;;

(def api-key "xMG25Dqse1Rij0y3xM2TeiVggdo1")

(defn request-and-parse-json [request]
  (json/parse-string (:body request) true))

(defn request-cric-resource [api & [params]]
  ;; (println "Request " api "with params" params)
  (request-and-parse-json
   (client/post (str  "http://cricapi.com/api/" api)
                {:form-params (merge {:apikey api-key} params)
                 :content-type :json})))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; match-list maintenance ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def match-list (atom []))

(defn get-matches []
  (:matches (request-cric-resource "matches")))


(defn fetch-match-list! []
  (reset! match-list (map-indexed (fn [i match]
                                    (assoc match :num i))
                                  (get-matches))))


(defn get-score [match-id]
  (:score (request-cric-resource "cricketScore"
                                 {:unique_id match-id})))

;;;;;;;;;;;;;;;;;;
;; notification ;;
;;;;;;;;;;;;;;;;;;

(def events (atom {}))

(defn send-notification [string]
  (sh "notify-send" string))

;; (defn run-notifications [dt]
;;   (println "just siome things"))

(defn start-event [event]
  (tick.schedule/start event (tick.clock/clock-ticking-in-seconds)))

(defn create-schedule [fn id]
  (let [event (sched/schedule fn
                              (take 10
                                    (timeline
                                     (periodic-seq (now)
                                                   (seconds 15)))))]
    (start-event event)
    (swap! events assoc (keyword id) event)))

(defn stop-schedule [id]
  (tick.schedule/stop ((keyword id) @events)))

;; (create-schedule run-notifications "134")

;; (stop-schedule "134")

;;;;;;;;;;;;;;;;;;;;;;;;
;; presentation stuff ;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn imp-matches [match-list]
  (filter (fn [match]
            (or (.contains good-teams (:team-1 match))
                (.contains good-teams (:team-2 match))))
          match-list))

(defn make-string [map-fn list]
  (reduce (fn [agg e]
            (str agg  (map-fn e) "\n")) "" list))

(defn match-list-string [match-list]
  (str "Current Matches:"
       "\n"
       (make-string (fn  [match]
                      (str (:num match)
                           ". "
                           (:team-1 match)
                           " is playing "
                           (:team-2 match)))
                    match-list)))

(defn command-docs [commands]
  (str "Available commands are: "
       "\n"
       (make-string (fn [cmd]
                      (str (:command cmd)
                           "\t"
                           (:doc cmd)))  commands)))


;;;;;;;;
;; IO ;;
;;;;;;;;

(defn get-match-by-num [num match-list]
  (first (filter #(= (:num %) (Integer/valueOf num)) match-list)))

(defn one-off [selected-match]
  (if-let [score (get-score (:unique_id (get-match-by-num selected-match
                                                          @match-list)))]
    (send-notification score)
    (println "Could not get response from server")))

(defn print-imp-matches []
   (println (match-list-string (imp-matches @match-list))))

(defn print-all-matches []
  (println (match-list-string @match-list)))


;;;;;;;;;;;;
;; driver ;;
;;;;;;;;;;;;

(def commands [{:command "start <num>"
                :name "start"
                :op (fn [[num]]
                      (create-schedule (fn [clock]
                                         (one-off num))
                                       num))
                :doc "Start notifications for match num"}
               {:command "stop <num>"
                :name "stop"
                :doc "Stop notifications for match num"
                :op (fn [[num]]
                      (stop-schedule num))}
               {:command "match-list"
                :name "match-list"
                :doc "Display all matches"
                :op (fn [& params]
                      (fetch-match-list!)
                      (println (match-list-string
                                (imp-matches @match-list))))}])


(defn execute! [command & [params]]
  ;; (println "executing " (:name command) params)
  ((:op command) params))

(defn menu-handler [command]
  (let [[cmd & params] (clojure.string/split command #" ")]
    (execute! (first (filter #(= (:name %) cmd) commands))
              params)))


;; (menu-handler "start 6")

;; (menu-handler "match-list")

;; (menu-handler "stop 6")

(defn take-input []
  (print "\n>> ")
  (flush)
  (read-line))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (fetch-match-list!)
  (println (match-list-string (imp-matches @match-list)))
  (println (command-docs commands))
  (loop [input (take-input)]
    (menu-handler input)
    (println (command-docs commands))
    (recur (take-input))))
