; take-screenshot copied from https://gist.github.com/jhartikainen/2843727
(ns blitzcheat-ml.utils
  (:gen-class)
  (:import 
    (java.awt Rectangle Dimension Robot Toolkit)
    (java.awt.image BufferedImage)
    (java.io File IOException)
    (javax.imageio ImageIO)))

(defn take-screenshot []
  (let [screen (.getScreenSize (Toolkit/getDefaultToolkit)) 
        rt (new Robot)
        img (.createScreenCapture rt (new Rectangle (int (.getWidth screen)) (int (.getHeight screen))))]
    (ImageIO/write img "jpg" (new File (str (System/currentTimeMillis) ".jpg")))))
