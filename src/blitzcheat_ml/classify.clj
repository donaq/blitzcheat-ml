(ns blitzcheat-ml.classify
  (:gen-class)
  (:require [clojure.data.json :as json]
            [me.raynes.fs :as fs]
            [blitzcheat-ml.utils :as utils :refer [classesdir]])
  (:import [org.deeplearning4j.datasets.datavec RecordReaderDataSetIterator]
           [org.datavec.api.split FileSplit]
           [org.datavec.api.io.filters PathFilter]
           [org.datavec.api.writable Writable IntWritable]
           [org.datavec.api.io.labels ParentPathLabelGenerator]
           [org.datavec.image.recordreader ImageRecordReader]
           [org.datavec.image.transform ScaleImageTransform]
           [org.deeplearning4j.zoo.model LeNet]
           ))

(def h 640)
(def w 960)
(def c 3)
(def seed 42)

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


(defn -main
  [& args]
  (let [filesplit (FileSplit. (clojure.java.io/file classesdir) (java.util.Random. seed))
        splitted (.sample filesplit (noop-filter) (into-array Double/TYPE [80 20]))
        train-files (first splitted)
        test-files (second splitted)
        train-iterator (dataset-iterator train-files)
        input-shape (into-array (map int-array [[c h w]]))
        ]
    (prn (.getLabels train-iterator))
    (prn input-shape)
    ))
