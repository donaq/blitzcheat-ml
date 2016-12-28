; take-screenshot copied from https://gist.github.com/jhartikainen/2843727
(ns blitzcheat-ml.utils
  (:gen-class)
  (:import 
    (java.awt Rectangle Dimension Robot Toolkit)
    (java.awt.image BufferedImage)
    (java.io File IOException)
    (javax.imageio ImageIO)))

(defn take-screenshot []
  ; Note that this borks if you are using Gnome on Wayland. I had to switch to Xorg for it to work. https://bbs.archlinux.org/viewtopic.php?id=220820
  (let [screen (.getScreenSize (Toolkit/getDefaultToolkit)) 
        rt (new Robot)
        img (.createScreenCapture rt (new Rectangle (int (.getWidth screen)) (int (.getHeight screen))))]
    ;TODO: we will need to return the image names
    (ImageIO/write img "jpg" (new File (str (System/currentTimeMillis) ".jpg")))))
