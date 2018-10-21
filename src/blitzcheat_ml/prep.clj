(ns blitzcheat-ml.prep
  (:gen-class)
  (:require [clojure.data.json :as json]
            [me.raynes.fs :as fs]
            [blitzcheat-ml.utils :as utils :refer [picdir classdir between?]]))

(defn- reset-dir []
  "deletes and recreates classdir"
  (if (fs/directory? classdir)
    (fs/delete-dir classdir))
  (if (not (fs/directory? classdir))
    (fs/mkdir classdir)))

(defn- write-labels [setdir dat]
  "writes labels.json for this set (training, dev or test) into setdir"
  (if (not (fs/directory? setdir))
    (fs/mkdir setdir))
  (spit (str setdir "/" "labels.json") (json/write-str dat)))

(defn- copy-pics [setdir dat]
  "copies pics from raw to setdir for this set"
  (doseq [[picname cls] dat]
    (fs/copy (str picdir "/" picname) (str setdir "/" picname))))


(defn- write-sets [sets]
  (reset-dir)
  (doseq [[loc dat] sets]
    (let [setdir (str classdir "/" loc)]
      (write-labels setdir dat)
      (copy-pics setdir dat))))

(defn -main
  "this will split the raw data into training, dev and test sets"
  [& args]
  ; not the most efficient code i've written, but this is not about that, i guess
  (let [pics ((utils/get-existing-annotations) "pics")
        probs (map (fn [[k v]] [[k (v "class")] (rand)]) pics)
        trainingset (into (hash-map) (map first (filter (fn [e] (< (second e) 0.8)) probs)))
        devset (into (hash-map) (map first (filter (fn [e] (between? (second e) 0.8 0.9)) probs)))
        testset (into (hash-map) (map first (filter (fn [e] (>= (second e) 0.9)) probs)))]
    (write-sets {"train" trainingset "dev" devset "test" testset})))
