(ns blitzcheat-ml.core
  (:gen-class)
  (:require [clojure.data.json :as json]
            [blitzcheat-ml.policy-gradient :as pg]
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
; last recorded score; this is the value we want to maximise. we will use score diffs to train our PG network
(def last-score (atom 0))

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
  ;(println @extension-dat)
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
  (let [gfilter (GrayFilter. true 10)
        producer (FilteredImageSource. (.getSource img) gfilter)
        tki (.createImage (Toolkit/getDefaultToolkit) producer)
        bufferedimg (BufferedImage. (.getWidth tki) (.getHeight tki) BufferedImage/TYPE_BYTE_GRAY)]
    (.drawImage (.getGraphics bufferedimg) tki, 0, 0, nil)
    bufferedimg))

(defn score-from-img [img tess]
  "tries to get score from img"
  (let [txt (.doOCR tess img)
        numstr (.replaceAll txt "[^0-9]" "")]
    (if (= "(©)\n" txt)
      0
      (if (blank? numstr)
        nil
        (Integer. numstr)))))

(defn game-pixels [bimg]
  "returns the 64 pixel values at the middle of each square on the board"
  (let [h (/ (.getHeight bimg) 8)
        w (/ (.getWidth bimg) 8)
        xoff (/ w 2)
        yoff (/ h 2)]
    (for [y (map (fn [i] (+ (* i h) yoff)) (range 8))
          x (map (fn [i] (+ (* i w) xoff)) (range 8))]
      (double (.getRGB bimg x y)))))

(defn gemcoords [sq-size sq-mid bx by x y]
  "get on screen coordinates of gem at row y col x"
  [
    (+ bx (* sq-size x) sq-mid)
    (+ by (* sq-size y) sq-mid)
  ])

(defn switch-gems [^Robot rt bx by sq-size sq-mid sx sy dx dy]
  "click on gem at row sy, col sx followed by gem at row dy col dx"
  (let [[x1 y1] (gemcoords sq-size sq-mid bx by sx sy)
        [x2 y2] (gemcoords sq-size sq-mid bx by dx dy)]
    (utils/click-on rt x1 y1)
    (utils/click-on rt x2 y2)))

(defn moves [^Robot rt actions bx by bw bh]
  "do all possible moves in actions"
  (let [iact (vec (.toIntVector actions))
        ; ok this is hardcoded to 16 because we have ensured that the board is square and making this
        ; super portable is not the bloody point
        sq-size (/ bw 8)
        sq-mid (/ bw 16)]
    (dorun (for [i (doall (range 64))
          :let [svstart (* i 4)
                svend (+ svstart 4)
                x (rem i 8)
                y (quot i 8)
                [up down left right] (subvec iact svstart svend)]]
      ; so there is a preferred direction
      (cond
        (and (not (= y 0)) (= up 1)) (switch-gems rt bx by sq-size sq-mid x y x (- y 1))
        (and (not (= y 7)) (= down 1)) (switch-gems rt bx by sq-size sq-mid x y x (+ y 1))
        (and (not (= x 0)) (= left 1)) (switch-gems rt bx by sq-size sq-mid x y (- x 1) y)
        (and (not (= x 7)) (= right 1)) (switch-gems rt bx by sq-size sq-mid x y (+ x 1) y))))))

(defn play-game [img board-area pgmod rt bx by bw bh]
  (let [bimg (get-area img board-area)
        pixels (double-array (game-pixels bimg))]
    ; feed model pixels and get actions
    (moves rt (pg/ff pgmod pixels) bx by bw bh)
    (Thread/sleep 1000)
  ))

(defn to-yes-no [ui]
  (cond
    (= ui "n") ui
    (= ui "y") ui
    :else
    (let [numstr (.replaceAll ui "[^0-9]" "")]
      (if (= numstr "")
        "n"
        (do
          (reset! last-score (Integer. numstr)) 
          (println "last-score reset to" @last-score)
          "y")))))

; legacy from score-based training
(defn train-or-discard [pgmod]
  (let [userinput (read-line)
        yn (to-yes-no userinput)]
    (if (= yn "y")
      (do
        (println "feed last-score and number of frames to the network for backprop")
        (pg/backprop pgmod @last-score))
      (pg/discard))))

(defn player-thread []
  "returns game playing function"
  (let [classifier (utils/load-class-model)
        gcls (get-game-cls)
        board-area ((gcls "areas") "board")
        rt (Robot.)
        pgmod (pg/get-model)]
    (utils/pre-game)
    (loop []
      (let [dat @extension-dat]
        (if (not (nil? dat))
          (let [img (utils/screenshot-img-from-dat dat)
                ; offset of board from screen
                bx (+ (int (board-area "x")) (int (dat "left")))
                by (+ (int (board-area "y")) (int (dat "top")))
                bw (int (board-area "width"))
                bh (int (board-area "height"))]
            (if (utils/is-game? img classifier)
              ; one iteration of play
              (play-game img board-area pgmod rt bx by bw bh)
              ; game finished
              (pg/backprop pgmod)))))
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
      (utils/click-on rt (first cp) (second cp))
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
