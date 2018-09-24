(ns blitzcheat-ml.prep
  (:gen-class)
  (:require [clojure.data.json :as json]
            [blitzcheat-ml.utils :as utils]))

(defn between?
  [x y z]
  (and (>= x y) (< x z)))

(defn -main
  "this will split the raw data into training, dev and test sets"
  [& args]
  (let [pics (keys ((utils/get-existing-annotations) "pics"))
        probs (map (fn [k] [k (rand)]) pics)
        trainingset (map first (filter (fn [e] (< (second e) 0.8)) probs))
        devset (map first (filter (fn [e] (between? (second e) 0.8 0.9)) probs))
        testset (map first (filter (fn [e] (>= (second e) 0.9)) probs))]
    (prn (count pics))
    (prn (count trainingset))
    (prn (count devset))
    (prn (count testset))))
