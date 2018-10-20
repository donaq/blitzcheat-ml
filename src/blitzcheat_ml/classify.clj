(ns blitzcheat-ml.classify
  (:gen-class)
  (:require [clojure.data.json :as json]
            [me.raynes.fs :as fs]
            [blitzcheat-ml.utils :as utils])
  (:import [org.deeplearning4j.datasets.iterator.impl EmnistDataSetIterator EmnistDataSetIterator$Set]
           [org.datavec.api.split FileSplit]
           [org.datavec.image.recordreader ImageRecordReader]
           ))

(defn -main
  [& args]
  (let [rr (ImageRecordReader. 640 960 3)]
    (.initialize rr (FileSplit. (clojure.java.io/file "raw")))
    (while (.hasNext rr)
      (do
        (.next rr)
        (prn (.getCurrentFile rr))))
    ))
