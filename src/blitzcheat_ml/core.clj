(ns blitzcheat-ml.core
  (:gen-class)
  (:require [clojure.data.json :as json]
            [blitzcheat-ml.utils :as utils])
  (:import [java.awt Robot Toolkit]
           [java.io File IOException]
           [javax.swing GrayFilter]
           [net.sourceforge.tess4j Tesseract1 Tesseract]
           [javax.imageio ImageIO]
           [java.awt.image BufferedImage FilteredImageSource])
  (:use [compojure.route :only [files not-found]]
        [clojure.string :only [blank?]]
        [compojure.handler :only [site]] ; form, query params decode; cookie; session, etc
        [compojure.core :only [defroutes GET POST DELETE ANY context]]
        [org.httpkit.server]))

; extension continuously writes to this. checker continuously sets this to nil
(def extension-dat (atom nil))
; how often the checker thread executes
(def checkerdelay 50)
; how big a diff between timestamp in extension-dat and current time before we set to nil
(def checkerdiff (* checkerdelay 4))
; how often worker thread executes
(def workerdelay (* checkerdelay 10))

(defn checker-thread []
  "checks extension-dat's timestamp. sets it to nil if it is more than checkerdiff from current timestamp"
  (let [dat @extension-dat]
    (if (and (not (nil? dat))
              (> (- (System/currentTimeMillis) (dat "timestamp"))
                checkerdiff))
      (reset! extension-dat nil)))
  (Thread/sleep checkerdelay)
  (recur))

(defn debugger-thread []
  (Thread/sleep 10000)
  (println @extension-dat)
  (recur))

(defn worker-thread [worker]
  (fn []
    (loop []
      (let [dat @extension-dat]
        (if (not (nil? dat))
          (worker dat)))
      (Thread/sleep workerdelay)
      (recur))))

(defn get-game-cls []
  (-> utils/datfile
      slurp
      json/read-str
      ((fn [annotations] (annotations "classes")))
      (->>
        (filter (fn [e] (= (e "name") "game"))))
      first))

(defn get-area [img spec]
  (.getSubimage img (int (spec "x")) (int (spec "y")) (int (spec "width")) (int (spec "height"))))

(defn grayscale [img]
  (let [gfilter (GrayFilter. false 50)
        producer (FilteredImageSource. (.getSource img) gfilter)
        tki (.createImage (Toolkit/getDefaultToolkit) producer)
        bufferedimg (BufferedImage. (.getWidth tki) (.getHeight tki) BufferedImage/TYPE_BYTE_GRAY)]
    (.drawImage (.getGraphics bufferedimg) tki, 0, 0, nil)
    bufferedimg))

(defn score-from-img [img tess]
  "tries to get score from img"
  (let [txt (.doOCR tess img)
        numstr (.replaceAll txt "[^0-9]" "")]
    (if (blank? numstr)
      nil
      (Integer. numstr))))

(defn play-game [img score-area board-area tess]
  (let [simg (grayscale (get-area img score-area))
        bimg (get-area img board-area)
        score (score-from-img simg tess)]
  ;(ImageIO/write simg "png" (File. "tmp/score.png"))
  (println "score is" score)
  ))

(defn player-thread []
  "returns game playing function"
  (let [classifier (utils/load-model)
        gcls (get-game-cls)
        score-area ((gcls "areas") "score")
        board-area ((gcls "areas") "board")
        tess (Tesseract1.)
        ;TODO: add game model
        ]
    (utils/pre-game)
    (.setLanguage tess "eng")
    ; hardcoded for what appears in arch after you install tesseract
    (.setDatapath tess "/usr/share/tessdata/")
    (loop []
      (let [dat @extension-dat]
        (if (not (nil? dat))
          (let [img (utils/screenshot-img-from-dat dat)]
            (if (utils/is-game? img classifier)
              ;TODO one iteration of play
              (play-game img score-area board-area tess)
              ;TODO: game finished action?
              ))))
      (Thread/sleep workerdelay)
      (recur))))

(defn handle-receive [data]
  "receives data over websocket and sets it to extension-dat"
  (let [dat (json/read-str data)]
    (reset! extension-dat (assoc dat "timestamp" (System/currentTimeMillis)))))

(defn get-click-points []
  "gets click points for all classes from annotations.json"
  (-> utils/datfile
      slurp
      json/read-str
      ((fn [annotations] (annotations "classes")))
      (->>
        (filter (fn [e] (contains? e "click")))
        (map (fn [c] (c "click"))))))

(defn click-to-game [dat click-points]
  "click through to a game: this does not currently work"
  (let [points (map (fn [cp] [(int (+ (dat "left") (first cp))) (int (+ (dat "top") (second cp)))]) click-points)
        rt (Robot.)]
    (doseq [cp points]
      (utils/click-on (first cp) (second cp) rt)
      (Thread/sleep 2000))))

(defn get-worker [mode]
  "based on mode, returns the function that does actual work, e.g. taking screenshots"
  (cond
    (= mode "gather") (do
                        (utils/pre-gather)
                        utils/take-screenshot)
    :else (fn [dat] nil)))

(defn ws-handler [request]
  "handler of websocket"
  (with-channel request channel
      (on-close channel (fn [status]
        (println "channel closed: " status)))
      (on-receive channel handle-receive)))

(defn ls-handler [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/write-str (utils/ls-raw))})

(defn annotate-pic [request]
  (utils/save-to-existing request)
  {:status 200})

(defroutes all-routes
  (GET "/ws" [] ws-handler)
  (GET "/ls" [] ls-handler)
  (POST "/annotate" [] annotate-pic)
  (files "/raw/" {:root "raw"})
  (files "/" {:root "public"}))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [mode (nth args 0)
        worker (get-worker mode)
        t1 (future (checker-thread))
        t2 (future ((worker-thread worker)))
        t3 (future (debugger-thread))
        t4 (if (= mode "play") (future (player-thread)))]
  ;(utils/take-screenshot)
    (println "Starting server on port 9999")
    ;(run-server (handler worker) {:port 9999})))
    (run-server (site #'all-routes) {:port 9999})))
