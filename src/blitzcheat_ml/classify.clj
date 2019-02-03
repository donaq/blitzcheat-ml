(ns blitzcheat-ml.classify
  (:gen-class)
  (:require [clojure.data.json :as json]
            [me.raynes.fs :as fs]
            [blitzcheat-ml.utils :as utils :refer [classesdir]])
  (:import [org.deeplearning4j.datasets.datavec RecordReaderDataSetIterator]
           [org.datavec.api.split FileSplit]
           [org.datavec.api.io.filters PathFilter RandomPathFilter]
           [org.datavec.api.writable Writable IntWritable]
           [org.datavec.api.io.labels ParentPathLabelGenerator]
           [org.datavec.image.recordreader ImageRecordReader]
           [org.datavec.image.transform ScaleImageTransform]
           [org.deeplearning4j.datasets.iterator MultipleEpochsIterator]
           [org.deeplearning4j.optimize.listeners ScoreIterationListener PerformanceListener]
           [org.deeplearning4j.optimize.api TrainingListener]
           [org.deeplearning4j.zoo.model LeNet]
           ))

(def h 640)
(def w 960)
(def c 3)
(def seed 23)

;(defn dataset-iterator [setname]
;  "returns a data set iterator based on setname (one of train, dev, test)"
;  (let [batch-size 10
;        rr (ImageRecordReader. h w c (set-label-gen setname))]
;    (.initialize rr (FileSplit. (clojure.java.io/file classesdir)))
;    ;rr))
;    (RecordReaderDataSetIterator. rr batch-size)))

(defn noop-filter []
  (reify
    PathFilter
    (#^"[Ljava.net.URI;" filter
      [this #^"[Ljava.net.URI;" paths]
      paths)))

(defn dataset-iterator [dataset]
  (let [batch-size 10
        rr (ImageRecordReader. h w c (ParentPathLabelGenerator.))]
    (.initialize rr dataset)
    (RecordReaderDataSetIterator. rr batch-size)))

(defn get-model [input-shape num-classes]
  (let [model (-> (LeNet/builder)
                    (.numClasses num-classes)
                    (.seed seed)
                    .build)]
    (.setInputShape model input-shape)
    (.init model)))


(defn -main
  [& args]
  (let [filesplit (FileSplit. (clojure.java.io/file classesdir) (java.util.Random. seed))
        splitted (.sample filesplit (RandomPathFilter. (java.util.Random. seed) (into-array String ["jpg"])) (into-array Double/TYPE [80 20]))
        train-files (first splitted)
        test-files (second splitted)
        train-iterator (dataset-iterator train-files)
        test-iterator (dataset-iterator test-files)
        input-shape (into-array (map int-array [[c w h]]))
        num-epochs 5
        each-iterations 50
        model (get-model input-shape (count (.getLabels train-iterator)))
        ]
    (.addListeners model (into-array TrainingListener [(PerformanceListener. 1) (ScoreIterationListener. each-iterations)]))
    (.fit model (MultipleEpochsIterator. num-epochs train-iterator))
    (println (-> model (.evaluate test-iterator) .stats))
    ))
