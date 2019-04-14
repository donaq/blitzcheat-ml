(ns blitzcheat-ml.policy-gradient
  (:gen-class)
  (:require [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [blitzcheat-ml.utils :as utils])
  (:use [clojure.java.io :only [file]])
  (:import 
    [org.nd4j.linalg.factory Nd4j]
    [org.nd4j.linalg.ops.transforms Transforms]
    [org.deeplearning4j.nn.conf NeuralNetConfiguration NeuralNetConfiguration$Builder]
    [org.deeplearning4j.nn.conf.layers OutputLayer$Builder DenseLayer DenseLayer$Builder]
    [org.nd4j.linalg.activations Activation]
    [org.nd4j.linalg.learning.config RmsProp]
    [org.nd4j.linalg.lossfunctions LossFunctions$LossFunction]
    [org.deeplearning4j.nn.weights WeightInit]
	[org.deeplearning4j.nn.multilayer MultiLayerNetwork]))

(def modpath "models/pg.model")
(def nin 64)
; 4 directions for each position
(def nout (* 64 4))
(def frames (atom []))

(defn to-nd4j-pixels [pixels]
  "convert java array of doubles to ndarray"
  ; 1e-6 is a magic value to ensure the result of applying sigmoid to nout outputs results in usable probabilities
  (.muli (Nd4j/create pixels) 1e-6))

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
                     (.layer (-> (DenseLayer$Builder.)
                                   (.nIn nin)
                                   (.nOut nout)
                                   (.activation Activation/IDENTITY)
                                   .build))
                     (.backprop true)
                     (.pretrain false)
                     .build)
            model (MultiLayerNetwork. conf) ]
        (.init model)
        model)))

(defn to-actions [aprobs]
  "gets output probabilities (64*4 ndarray) and returns actions"
  (let [sampler (Nd4j/rand (.shape aprobs))]
    ; the higher the probability of the action, the more likely it is the sampler is less than it
    (Transforms/lessThanOrEqual sampler aprobs)))

(defn ff [m pixels]
  "feed-forward pixels"
  ;TODO: record pixels i.e. input for backprop
  (let [frame (to-nd4j-pixels pixels)]
    (reset! frames (conj @frames frame))
    (.setInput m frame)
    ;TODO: record output
    (let [acts (.feedForward m)
          aprobs (Transforms/sigmoid (last acts))]
      (to-actions aprobs))))

(defn backprop [m score]
  "backprop on model"
  ;TODO: actually update model
  (reset! frames [])
  (println @frames)
  (println "finished updating model")
  m)
