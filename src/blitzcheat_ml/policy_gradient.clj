(ns blitzcheat-ml.policy-gradient
  (:gen-class)
  (:require [me.raynes.fs :as fs]
            [clojure.java.io :as io])
  (:use [clojure.java.io :only [file]])
  (:import 
    [org.nd4j.linalg.factory Nd4j]
	[org.deeplearning4j.nn.multilayer MultiLayerNetwork]))

(defn to-nd4j-pixels [pixels]
  (Nd4j/create pixels))
