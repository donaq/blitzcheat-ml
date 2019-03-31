(ns blitzcheat-ml.utils
  (:gen-class)
  (:require [me.raynes.fs :as fs]
            [image-resizer.core :as iresize]
            [image-resizer.format :as iformat]
            [clojure.java.io :as io]
            [clojure.data.json :as json])
  (:use [clojure.java.io :only [file]])
  (:import 
    [java.awt Rectangle Robot]
    [java.awt.event InputEvent]
    [java.awt.image BufferedImage]
    [java.io File IOException]
    [javax.imageio ImageIO]
    [org.datavec.image.loader ImageLoader]
	[org.deeplearning4j.nn.multilayer MultiLayerNetwork]))

; all the constants
(def picdir "raw/")
(def datfile "public/annotations.json")
(def classesdir "learn-classes/")

; ensmallen constants
(def cropx 270)
(def cropy 50)
(def cropw 420)
(def croph 590)
(def resizew 84)
(def resizeh 118)
; tmp directory. this is for DRY, not abbreviation
(def tmpdir "tmp")

(defn load-class-model []
  (MultiLayerNetwork/load (io/file "models/trained.model") true))

(defn ensmallen [from to]
  "resizes an image file 'from' to width and height and writes it to 'to'"
  (iformat/as-file
    (-> (file from)
      (iresize/crop-from cropx cropy cropw croph)
      (iresize/force-resize resizew resizeh))
    to :verbatim))

(defn ensure-dir [dirname]
  "creates directory by name of dirname if it doesn't exist"
  (if (not (fs/directory? dirname))
    (fs/mkdir dirname)))

(defn pre-gather []
  (println "pre-gather")
  (ensure-dir picdir))

(defn pre-game []
  (ensure-dir tmpdir))

(defn req-json [request]
  ; parses (:body request) as json
  (json/read-str (slurp (:body request) :encoding "UTF-8")))

(defn save-to-existing [request]
  (let [jstr (slurp (:body request) :encoding "UTF-8")]
    (println jstr)
    (spit datfile jstr)))

(defn get-existing-annotations []
  "returns the hashmap in datfile if it exists, otherwise returns empty map"
  (if (fs/file? datfile)
    (-> datfile slurp json/read-str)
    {"pics" {}}))

(defn from-dir []
  "returns a hashmap of <image file name>:{} from picdir"
  (into {}
          (map (fn [v] [v {}]) (fs/list-dir picdir))))

(defn merge-lses [existing lsres]
  "returns a map with the keys in `lsres` taking values from `existing` iff the corresponding key exists"
  (merge lsres
         (into {} (filter #(contains? lsres (first %)) existing))))

(defn ls-raw []
  (let [existing (get-existing-annotations)
        newpics (merge-lses (existing "pics") (from-dir))]
    (merge existing {"pics" newpics})))

(defn screenshot-img-from-dat [dat]
  (let [x (int (dat "left"))
        y (int (dat "top"))
        width (int (dat "width"))
        height (int (dat "height"))
        rt (new Robot)]
    (.createScreenCapture rt (new Rectangle x y width height))))

; take-screenshot copied from https://gist.github.com/jhartikainen/2843727
(defn take-screenshot [dat]
  ; Note that this borks if you are using Gnome on Wayland. I had to switch to Xorg for it to work. https://bbs.archlinux.org/viewtopic.php?id=220820
  (let [fname (str picdir (System/currentTimeMillis) ".jpg")
        img (screenshot-img-from-dat dat)]
    ;TODO: we will need to return the image names
    (ImageIO/write img "jpg" (new File fname))
    (println "created " fname)))

(defn predict-fname [classier fname]
  (let [il (ImageLoader. resizeh resizew 3)
        rv (.asRowVector il (new File fname))]
        ;res (aget (.predict classier rv) 0)]
    (aget (.predict classier rv) 0)))

(defn is-game? [img classier]
  "predict if screen area specified by dat is a game. return true if it is"
  ; yes, we write files, since we trained the classifier using
  ; this process, I thought it best not to tamper.
  (let [fname "tmp/screenshot.jpg"
        smallname "tmp/small.jpg"]
    (ImageIO/write img "jpg" (new File fname))
    (ensmallen fname smallname)
    (= 1 (predict-fname classier smallname))))

(defn click-on [x y rt]
  (let [button (InputEvent/getMaskForButton 1)]
    (.mouseMove rt x y)
    ;(.mousePress rt button)
    ;(.mouseRelease rt button)
    ))
