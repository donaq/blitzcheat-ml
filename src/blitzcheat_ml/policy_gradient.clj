(ns blitzcheat-ml.policy-gradient
  (:gen-class)
  (:require [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [blitzcheat-ml.utils :as utils])
  (:use [clojure.java.io :only [file]]
        [clojure.string :only [trim split-lines]]
        )
  (:import 
    [java.lang Math]
    [org.nd4j.linalg.factory Nd4j]
    [org.nd4j.linalg.ops.transforms Transforms]
    [org.deeplearning4j.nn.conf NeuralNetConfiguration NeuralNetConfiguration$Builder]
    [org.deeplearning4j.nn.conf.layers OutputLayer$Builder DenseLayer DenseLayer$Builder]
    [org.deeplearning4j.nn.workspace LayerWorkspaceMgr]
    [org.nd4j.linalg.activations Activation]
    [org.nd4j.linalg.learning.config RmsProp]
    [org.nd4j.linalg.lossfunctions LossFunctions$LossFunction]
    [org.deeplearning4j.nn.weights WeightInit]
	[org.deeplearning4j.nn.multilayer MultiLayerNetwork]))

(def modpath "models/pg.model")
(def scorepath "models/pgscores")
(def prevscore (atom 0))
(def nin 64)
; 4 directions for each position
(def nout (* 64 4))
(def frames (atom []))
(def dlogps (atom []))
(def discount 0.99)

(defn to-nd4j-pixels [pixels]
  "convert java array of doubles to ndarray"
  ; 1e-6 is a magic value to ensure the result of applying sigmoid to nout outputs results in usable probabilities
  (.muli (Nd4j/create pixels) 1e-6))

(defn load-model []
  (MultiLayerNetwork/load (io/file modpath) true))

(defn save-model [^MultiLayerNetwork model]
  (utils/ensure-dir "models")
  (.save model (io/file modpath) true))

(defn discard []
  (reset! frames [])
  (reset! dlogps [])
  (println "discarded this game"))

(defn get-model []
  ; get-model must set previous score
  (if (fs/file? modpath)
      (load-model)
      (let [conf (-> (NeuralNetConfiguration$Builder.)
                     (.seed 42)
                     (.activation Activation/RELU)
                     (.weightInit WeightInit/XAVIER)
                     (.updater (RmsProp. 1e-4))
                     .list
                     (.layer (-> (DenseLayer$Builder.)
                                   (.nIn nin)
                                   (.nOut nin)
                                   .build))
                     (.layer (-> (DenseLayer$Builder.)
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

(defn to-actions [aprobs]
  "gets output probabilities (64*4 ndarray) and returns actions"
  (let [sampler (Nd4j/rand (.shape aprobs))]
    ; the higher the probability of the action, the more likely it is the sampler is less than it
    (Transforms/lessThanOrEqual sampler aprobs)))

(defn ff [m pixels]
  "feed-forward pixels"
  (let [frame (to-nd4j-pixels pixels)]
    (.setInput m frame)
    ;TODO: record output
    (let [acts (.feedForward m)
          aprobs (last acts)
          actions (to-actions aprobs)
          dlogp (.sub actions aprobs)]
      ; record pixels i.e. input for backprop
      (reset! frames (conj @frames frame))
      (reset! dlogps (conj @dlogps dlogp))
      actions
      )))

(defn reward [oldscore newscore]
  (cond
    (= oldscore newscore) 0 ; at higher scores this is unlikely to happen probably
    ; scale advantage to how much we have improved or deproved, but we want a minimum advantage of 1
    (> newscore oldscore) (max 1 (Math/log10 (- newscore oldscore)))
    (< newscore oldscore) (* -1 (max 1 (Math/log10 (- oldscore newscore))))))

(defn discounted-rewards [r]
  (.transpose
    (Nd4j/create
      (double-array
        (for [i (reverse (range (count @frames)))]
          (* r (Math/pow discount i)))))))

(defn update-model [m minput merr]
  (.setInput m minput)
  (.feedForward m true false)
  (let [p (.backpropGradient m merr nil)
        grad (.getFirst p)
        iter (.getIterationCount m)
        epoch (.getEpochCount m)
        updater (.getUpdater m)]
    (println "epoch" epoch "iter" iter)
    (.update updater m grad iter epoch (count @frames) (LayerWorkspaceMgr/noWorkspaces))
    (.subi (.params m) (.gradient grad))
    (.setEpochCount m (+ 1 epoch))
    (.setIterationCount m 1)))

(defn print-params [m]
  (let [l1s (.params (.getLayer m 0))
        l2s (.params (.getLayer m 1))]
    (println (.getDouble l1s 20) (.getDouble l2s 20))))

(defn rewards-from-frames []
  "reward is -1 for a frame if the frame after that has no changes, otherwise it's 1"
  (doall (for [i (range (- (count @frames) 1))]
    (if (= 0.0
           (.getDouble (Nd4j/sum (.neq (nth @frames i) (nth @frames (+ 1 i)))) 0))
      -1
      1)
    )))

(defn backprop [m]
  "backprop on model"
  (if (> 2 (count @frames))
    m
    (let [rewards (rewards-from-frames)
          f-end (- (count @frames) 1)
          ;dlogps-concat (Nd4j/concat 0 (into-array (subvec @dlogps 0 f-end)))
          ;frames-concat (Nd4j/concat 0 (into-array (subvec @frames 0 f-end)))]
          ]
      (println rewards)
      ;TODO: actually update model
      ;(update-model m frames-concat (.muliColumnVector dlogps-concat (discounted-rewards r)))
      ;(update-model m frames-concat (.muliColumnVector dlogps-concat rewards))
      (println "finished updating model with" (count @frames) "frames")
      (save-model m)
      (discard)
      (println "saved model")
      (print-params m)
      m)))
