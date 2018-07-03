(ns blitzcheat-ml.core
  (:gen-class)
  (:require [clojure.data.json :as json]
            [blitzcheat-ml.utils :as utils])
  (:use [org.httpkit.server]))

(def called (atom false))

(defn gameplayer-thread [channel]
  ;TODO: take screenshots continuously
  (fn []
    (loop [unimportant nil]
      (Thread/sleep 1000)
      (if (not (and (websocket? channel) (open? channel)))
        (println "Channel is either not a websocket or it is closed. Exiting.")
        (recur (println "Do some work")))))


  ;TODO: figure out from the screenshot if we have an ongoing game. if yes, do reinforced learning on the game

  ;TODO: if we don't have an ongoing game, figure out if we can start a new one. this is probably going to be one of 3 screens. if it is not within the set of screens for which we can continue playing the game, we do nothing.
)

(defn handle-receive [data]
  (let [dat (json/read-str data)]
    (utils/take-screenshot dat)))
    ;(utils/mouseto dat)))

(defn get-receiver [mode]
  (cond (= mode "gather") handle-receive
        :else handle-receive))

(defn handler [receiver]
  (fn [request]
    (with-channel request channel
      ;TODO: create a game playing object
      (let [f (future ((gameplayer-thread channel)))]
        (on-close channel (fn [status]
          ;TODO: stop game playing object
          (println "channel closed: " status)))
        ; for completeness we include an on-receive, but we really don't care what is received
        ; ok, for completeness and also it's nice to see if stuff is still happening
        (on-receive channel receiver)))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [mode (nth args 0)
        receiver (get-receiver mode)]
  ;(utils/take-screenshot)
    (println "Starting server on port 9999")
    (run-server (handler receiver) {:port 9999})))
