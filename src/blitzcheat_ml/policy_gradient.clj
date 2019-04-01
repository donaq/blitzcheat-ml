(ns blitzcheat-ml.policy-gradient
  (:gen-class)
  (:require [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [blitzcheat-ml.utils :as utils])
  (:use [clojure.java.io :only [file]])
  (:import 
    [org.nd4j.linalg.factory Nd4j]
    [org.deeplearning4j.nn.conf NeuralNetConfiguration NeuralNetConfiguration$Builder]
    [org.deeplearning4j.nn.conf.layers OutputLayer$Builder DenseLayer DenseLayer$Builder]
    [org.nd4j.linalg.activations Activation]
    [org.nd4j.linalg.learning.config RmsProp]
    [org.nd4j.linalg.lossfunctions LossFunctions$LossFunction]
    [org.deeplearning4j.nn.weights WeightInit]
	[org.deeplearning4j.nn.multilayer MultiLayerNetwork]))

(def modpath "models/pg.model")
(def nin 64)
(def nout (* 64 5))

(defn to-nd4j-pixels [pixels]
  "convert java array of doubles to ndarray"
  (Nd4j/create pixels))

(defn load-model []
  (MultiLayerNetwork/load (io/file modpath) true))

(defn save-model [^MultiLayerNetwork model]
  (utils/ensure-dir "models")
  (.save model (io/file modpath) true))

(defn get-model []
  (if (fs/file? modpath)
      (load-model)
      (let [conf (-> (NeuralNetConfiguration$Builder.)
                     (.seed 42)
                     (.activation Activation/RELU)
                     (.weightInit WeightInit/XAVIER)
                     (.updater (RmsProp.))
                     .list
                     (.layer (-> (DenseLayer$Builder.)
                                   (.nIn nin)
                                   (.nOut nin)
                                   .build))
                     (.layer (-> (OutputLayer$Builder. LossFunctions$LossFunction/XENT)
                                   (.nIn nin)
                                   (.nOut nout)
                                   (.activation Activation/SIGMOID)
                                   .build))
                     (.backprop true)
                     (.pretrain false)
                     .build)
            model (MultiLayerNetwork. conf) ]
        (.init model)
        model)))
