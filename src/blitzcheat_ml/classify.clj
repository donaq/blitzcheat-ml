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
      (^boolean inferLabelClasses [this]
        false)
      )))



(defn -main
  [& args]
  (let [setname "train"
        setdir (utils/setdir-from-set setname)
        rr (ImageRecordReader. 640 960 3 (set-label-gen setname))]
    (.initialize rr (FileSplit. (clojure.java.io/file setdir)))
    (while (.hasNext rr)
      (do
        (prn (.next rr))
        ))
    ))
