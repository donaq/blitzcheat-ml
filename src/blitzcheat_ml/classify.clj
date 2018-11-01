(ns blitzcheat-ml.classify
  (:gen-class)
  (:require [clojure.data.json :as json]
            [me.raynes.fs :as fs]
            [blitzcheat-ml.utils :as utils])
  (:import [org.deeplearning4j.datasets.datavec RecordReaderDataSetIterator]
           [org.datavec.api.split FileSplit]
           [org.datavec.api.writable Writable IntWritable]
           [org.datavec.api.io.labels PathLabelGenerator]
           [org.datavec.image.recordreader ImageRecordReader]
           ))

(defn set-label-gen [setname]
  (let [labels (json/read-str (slurp (utils/set-labels-fname setname)))]
    (reify
      PathLabelGenerator
      (^Writable getLabelForPath [this ^String path]
        (IntWritable. (labels (-> path (clojure.string/split #"\/") last))))
      (^Writable getLabelForPath [this ^java.net.URI uri]
        (.getLabelForPath this (-> uri clojure.java.io/file .toString)))
      (^boolean inferLabelClasses [this]
        true)
      )))

(defn dataset-iterator [setname]
  "returns a data set iterator based on setname (one of train, dev, test)"
  (let [batch-size 10
        setdir (utils/setdir-from-set setname)
        rr (ImageRecordReader. 640 960 3 (set-label-gen setname))]
    (.initialize rr (FileSplit. (clojure.java.io/file setdir)))
    ;rr))
    (RecordReaderDataSetIterator. rr batch-size)))


(defn -main
  [& args]
  (let [blitz-train (dataset-iterator "train")
        blitz-test (dataset-iterator "test")
        blitz-dev (dataset-iterator "dev")]
    (prn (.getLabels blitz-train))
    (prn (.getLabels blitz-test))
    (prn (.getLabels blitz-dev))
    ))
