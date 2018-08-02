; take-screenshot copied from https://gist.github.com/jhartikainen/2843727
(ns blitzcheat-ml.utils
  (:gen-class)
  (:require [me.raynes.fs :as fs]
            [clojure.data.json :as json])
  (:import 
    (java.awt Rectangle Dimension Robot Toolkit)
    (java.awt.image BufferedImage)
    (java.io File IOException)
    (javax.imageio ImageIO)))

(def picdir "raw/")
(def datfile "public/annotations.json")

(defn pre-gather []
  (println "pre-gather")
  (if (not (fs/directory? picdir))
    (fs/mkdir picdir)))

(defn req-json [request]
  (json/read-str (slurp (:body request) :encoding "UTF-8")))

(defn get-existing-annotations []
  "returns the hashmap in datfile if it exists, otherwise returns empty map"
  (if (fs/file? datfile)
    (-> datfile slurp json/read-str)
    {}))

(defn from-dir []
  "returns a hashmap of <image file name>:{} from picdir"
  (into {}
          (map (fn [v] [v {}]) (fs/list-dir picdir))))

(defn merge-lses [existing lsres]
  "returns a map with the keys in `lsres` taking values from `existing` iff the corresponding key exists"
  (merge lsres
         (into {} (filter #(contains? lsres (first %)) existing))))

(defn ls-raw []
  (json/write-str (merge-lses (get-existing-annotations) (from-dir))))

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
