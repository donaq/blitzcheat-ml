; take-screenshot copied from https://gist.github.com/jhartikainen/2843727
(ns blitzcheat-ml.utils
  (:gen-class)
  (:require [me.raynes.fs :as fs])
  (:import 
    (java.awt Rectangle Dimension Robot Toolkit)
    (java.awt.image BufferedImage)
    (java.io File IOException)
    (javax.imageio ImageIO)))

(def picdir "raw/")

(defn pre-gather []
  (println "pre-gather")
  (if (not (fs/directory? picdir))
    (fs/mkdir picdir)))

(defn ls-raw []
  (fs/list-dir picdir))

(defn take-screenshot [dat]
  ; Note that this borks if you are using Gnome on Wayland. I had to switch to Xorg for it to work. https://bbs.archlinux.org/viewtopic.php?id=220820
  (let [x (int (dat "left"))
        y (int (dat "top"))
        width (int (dat "width"))
        height (int (dat "height"))
        rt (new Robot)
        fname (str picdir (System/currentTimeMillis) ".jpg")
        img (.createScreenCapture rt (new Rectangle x y width height))]
    ;TODO: we will need to return the image names
    (ImageIO/write img "jpg" (new File fname))
    (println "created " fname)))

(defn mouseto [dat]
  (let [x (int (dat "left"))
        y (int (dat "top"))
        rt (new Robot)]
    (.mouseMove rt x y)))
