(ns blitzcheat-ml.core
  (:gen-class)
  (:require [blitzcheat-ml.utils :as utils])
  (:use [org.httpkit.server]))

(defn startgame []
  (println "startgame called"))

(defn handler [request]
  (with-channel request channel
    (on-close channel (fn [status] (println "channel closed: " status)))
    (on-receive channel (fn [data]
                          ;(send! channel data)
                          (println (resolve (symbol data)))
                          (apply (resolve (symbol data)) [])))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;(utils/take-screenshot)
  (println "Starting server on port 9999")
  (run-server handler {:port 9999}))
