(ns blitzcheat-ml.core
  (:gen-class)
  (:require [blitzcheat-ml.utils :as utils])
  (:use [org.httpkit.server]))

(defn gameplayer-thread []
  ;TODO: take screenshots continuously
  ;TODO: figure out from the screenshot if we have an ongoing game. if yes, do reinforced learning on the game
  ;TODO: if we don't have an ongoing game, figure out if we can start a new one. this is probably going to be one of 3 screens. if it is not within the set of screens for which we can continue playing the game, we do nothing.
  )

(defn handler [request]
  (with-channel request channel
    ;TODO: create a game playing object
    (on-close channel (fn [status]
      ;TODO: stop game playing object
      (println "channel closed: " status)))
    ; for completeness we include an on-receive, but we really don't care what is received
    ; ok, for completeness and also it's nice to see if stuff is still happening
    (on-receive channel (fn [data]
                          (println data)))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;(utils/take-screenshot)
  (println "Starting server on port 9999")
  (run-server handler {:port 9999}))
