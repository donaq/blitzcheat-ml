(ns blitzcheat-ml.core
  (:gen-class)
  (:require [clojure.data.json :as json]
            [blitzcheat-ml.utils :as utils])
  (:use [compojure.route :only [files not-found]]
        [compojure.handler :only [site]] ; form, query params decode; cookie; session, etc
        [compojure.core :only [defroutes GET POST DELETE ANY context]]
        [org.httpkit.server]))

; this is a simple lock to prevent flooding
(def extension-dat (atom nil))

; how often the checker thread executes
(def checkerdelay 50)
; how big a diff between timestamp in extension-dat and current time before we set to nil
(def checkerdiff (* checkerdelay 4))
; how often worker thread executes
(def workerdelay (* checkerdelay 10))

(defn checker-thread []
  "checks extension-dat's timestamp. sets it to nil if it is more than checkerdiff from current timestamp"
  (let [dat @extension-dat]
    (if (and (not (nil? dat))
              (> (- (System/currentTimeMillis) (dat "timestamp"))
                checkerdiff))
      (reset! extension-dat nil)))
  (Thread/sleep checkerdelay)
  (recur))

(defn debugger-thread []
  (Thread/sleep 10000)
  (println @extension-dat)
  (recur))

(defn gameplayer-thread [channel]
  ;TODO: take screenshots continuously
  (fn []
    (loop [unimportant nil]
      (Thread/sleep 1000)
      (if (not (and (websocket? channel) (open? channel)))
        (println "Channel is either not a websocket or it is closed. Exiting.")
        (recur nil))))
  ;TODO: figure out from the screenshot if we have an ongoing game. if yes, do reinforced learning on the game
)

(defn worker-thread [worker]
  (fn []
    (loop []
      (let [dat @extension-dat]
        (if (not (nil? dat))
          (worker dat)))
      (Thread/sleep workerdelay)
      (recur))))

(defn handle-receive [data]
  "receives data over websocket and sets it to extension-dat"
  (let [dat (json/read-str data)]
    (reset! extension-dat (assoc dat "timestamp" (System/currentTimeMillis)))))

(defn get-worker [mode]
  "based on mode, returns the function that does actual work, e.g. taking screenshots"
  (cond
    (= mode "gather") (do
                        (utils/pre-gather)
                        utils/take-screenshot)
    :else (fn [dat] nil)))

(defn ws-handler [request]
  "handler of websocket"
  (with-channel request channel
    ;TODO: create a game playing object
    (let [f (future ((gameplayer-thread channel)))]
      (on-close channel (fn [status]
        ;TODO: stop game playing object
        (println "channel closed: " status)))
      (on-receive channel handle-receive))))

(defn ls-handler [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/write-str (utils/ls-raw))})

(defn annotate-pic [request]
  (let [picdat (json/read-str (slurp (:body request) :encoding "UTF-8"))]
    (println picdat)))

(defroutes all-routes
  (GET "/ws" [] ws-handler)
  (GET "/ls" [] ls-handler)
  (POST "/annotate" [] annotate-pic)
  (files "/raw/" {:root "raw"}))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [mode (nth args 0)
        worker (get-worker mode)
        t1 (future (checker-thread))
        t2 (future ((worker-thread worker)))
        t3 (future (debugger-thread))]
  ;(utils/take-screenshot)
    (println "Starting server on port 9999")
    ;(run-server (handler worker) {:port 9999})))
    (run-server (site #'all-routes) {:port 9999})))
