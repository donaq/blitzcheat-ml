(ns blitzcheat-ml.prep
  (:gen-class)
  (:require [clojure.data.json :as json]
            [me.raynes.fs :as fs]
            [blitzcheat-ml.utils :as utils :refer [picdir classesdir]]))

(defn- reset-dir [numclasses]
  "deletes and recreates classesdir"
  (if (fs/directory? classesdir)
    (fs/delete-dir classesdir))
  (fs/mkdir classesdir)
  (doseq [i (range numclasses)]
    (fs/mkdir (str classesdir "/" i))))

(defn binary-class-dir [classes details]
  (if (= ((nth classes (details "class")) "name") "game")
    1
    0))

(defn -main
  "this will split the raw data into training, dev and test sets"
  [& args]
  ; not the most efficient code i've written, but this is not about that, i guess
  (let [dat (utils/get-existing-annotations)
        pics (dat "pics")
        classes (dat "classes")]
    (reset-dir 2)
    (doseq [[pic details] pics]
      (utils/ensmallen (str picdir "/" pic) (str classesdir "/" (binary-class-dir classes details) "/" pic)))))
