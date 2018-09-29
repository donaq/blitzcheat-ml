(ns blitzcheat-ml.prep
  (:gen-class)
  (:require [clojure.data.json :as json]
            [me.raynes.fs :as fs]
            [blitzcheat-ml.utils :as utils]))

(def classdir "learn-classes/")

(defn between?
  [x y z]
  (and (>= x y) (< x z)))

(defn write-sets
  [sets]
  (if (not (fs/directory? classdir))
    (fs/mkdir classdir))
  (doseq [[loc dat] sets]
    (spit (str classdir loc ".json") (json/write-str dat))))

(defn -main
  "this will split the raw data into training, dev and test sets"
  [& args]
  ; not the most efficient code i've written, but this is not about that, i guess
  (let [pics ((utils/get-existing-annotations) "pics")
        probs (map (fn [[k v]] [[k (v "class")] (rand)]) pics)
        trainingset (map first (filter (fn [e] (< (second e) 0.8)) probs))
        devset (map first (filter (fn [e] (between? (second e) 0.8 0.9)) probs))
        testset (map first (filter (fn [e] (>= (second e) 0.9)) probs))]
    (write-sets {"train" trainingset "dev" devset "test" testset})))
