(ns emnist.core
  (:gen-class)
  (:require [me.raynes.fs :as fs]
            [blitzcheat-ml.utils :as utils])
  (:import [org.deeplearning4j.datasets.iterator.impl EmnistDataSetIterator EmnistDataSetIterator$Set]
           [org.deeplearning4j.nn.conf NeuralNetConfiguration NeuralNetConfiguration$Builder]
           [org.deeplearning4j.nn.api OptimizationAlgorithm]
           [org.nd4j.linalg.learning.config Adam]
           [org.deeplearning4j.nn.conf.layers DenseLayer DenseLayer$Builder OutputLayer OutputLayer$Builder]
           [org.nd4j.linalg.activations Activation]
           [org.nd4j.linalg.lossfunctions LossFunctions]
           [org.deeplearning4j.nn.weights WeightInit]
           [org.deeplearning4j.nn.multilayer MultiLayerNetwork]
           [org.deeplearning4j.optimize.listeners ScoreIterationListener]
           [org.deeplearning4j.optimize.api TrainingListener]
           [org.deeplearning4j.datasets.iterator MultipleEpochsIterator]
           ))
;org.deeplearning4j.nn.graph
;org.deeplearning4j.nn.conf.inputs
;org.deeplearning4j.datasets.datavec RecordReaderMultiDataSetIterator
;org.deeplearning4j.eval Evaluation

(defn -main
  "this will split the raw data into training, dev and test sets"
  [& args]
  (let [batch-size 20
        emnist-set (EmnistDataSetIterator$Set/MNIST)
        emnist-train (EmnistDataSetIterator. emnist-set batch-size true)
        emnist-test (EmnistDataSetIterator. emnist-set batch-size false)
        output-num (EmnistDataSetIterator/numLabels emnist-set)
        rng-seed 123
        num-rows 28
        num-cols 28
        conf (-> (NeuralNetConfiguration$Builder.)
                 (.seed rng-seed)
                 (.updater (Adam.))
                 (.l2 1e-4)
                 .list
                 (.layer (-> (DenseLayer$Builder.)
                             (.nIn (* num-rows num-cols))
                             (.nOut 1000)
                             (.activation Activation/RELU)
                             (.weightInit WeightInit/XAVIER)
                             .build))
                 (.layer (-> (OutputLayer$Builder.)
                             (.nIn 1000)
                             (.nOut output-num)
                             (.activation Activation/SOFTMAX)
                             (.weightInit WeightInit/XAVIER)
                             .build))
                 (.pretrain false) (.backprop true)
                 .build)
        network (MultiLayerNetwork. conf)
        each-iterations 1000
        num-epochs 5]
    (.init network)
    (.addListeners network (into-array TrainingListener [(ScoreIterationListener. each-iterations)]))
    (.fit network (MultipleEpochsIterator. num-epochs emnist-train))
    (println (-> network (.evaluate emnist-test) .stats))
    (println (-> network (.evaluateROCMultiClass emnist-test) .stats))
    ))
