(ns blitzcheat-ml.core
  (:gen-class)
  (:require [clojure.data.json :as json]
            [blitzcheat-ml.utils :as utils])
  (:use [org.httpkit.server]))

; this is a simple lock to prevent flooding
(def called (atom false))

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

(defn handle-receive [func]
  "returns a function that converts data received from websocket to json and runs func on it"
  (fn [data]
    (let [dat (json/read-str data)]
      (if (compare-and-set! called false true)
        (do (func dat)
            (reset! called false))
        nil))))
    ;(utils/mouseto dat)))

(defn get-worker [mode]
  "based on mode, returns the function that does actual work, e.g. taking screenshots"
  (handle-receive
    (cond
      (= mode "gather") utils/take-screenshot
      :else (fn [dat] (println "do nothing")))))

(defn handler [worker]
  "returns actual handler of request"
  (fn [request]
    (with-channel request channel
      ;TODO: create a game playing object
      (let [f (future ((gameplayer-thread channel)))]
        (on-close channel (fn [status]
          ;TODO: stop game playing object
          (println "channel closed: " status)))
        ; for completeness we include an on-receive, but we really don't care what is received
        ; ok, for completeness and also it's nice to see if stuff is still happening
        (on-receive channel worker)))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [mode (nth args 0)
        worker (get-worker mode)]
  ;(utils/take-screenshot)
    (println "Starting server on port 9999")
    (run-server (handler worker) {:port 9999})))
