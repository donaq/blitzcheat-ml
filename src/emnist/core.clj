(ns emnist.core
  (:gen-class)
  (:require [me.raynes.fs :as fs]
            [blitzcheat-ml.utils :as utils])
  (:import ;[org.deeplearning4j.datasets.iterator]
           [org.deeplearning4j.datasets.iterator.impl EmnistDataSetIterator]))
;org.deeplearning4j.nn.api
;org.deeplearning4j.nn.multilayer
;org.deeplearning4j.nn.graph
;org.deeplearning4j.nn.conf
;org.deeplearning4j.nn.conf.inputs
;org.deeplearning4j.nn.conf.layers
;org.deeplearning4j.nn.weights
;org.deeplearning4j.optimize.listeners
;org.deeplearning4j.datasets.datavec RecordReaderMultiDataSetIterator
;org.deeplearning4j.eval Evaluation
;org.nd4j.linalg.learning.config
;org.nd4j.linalg.activations Activation
;org.nd4j.linalg.lossfunctions LossFunctions 

(defn -main
  "this will split the raw data into training, dev and test sets"
  [& args]
  (println "hello emnist"))
