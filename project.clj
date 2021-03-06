(defproject blitzcheat-ml "0.1.0-SNAPSHOT"
  :description "Bejeweled Blitz bot that learns to play the game using machine learning."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.json "0.2.6"]
                 [javax.servlet/servlet-api "2.5"]
                 [net.sourceforge.tess4j/tess4j "4.3.1"]
                 [org.nd4j/nd4j-native-platform "1.0.0-beta3"]
                 ;[org.nd4j/nd4j-cuda-9.2-platform "1.0.0-beta2"]
                 [org.deeplearning4j/deeplearning4j-core "1.0.0-beta3"]
                 [org.deeplearning4j/deeplearning4j-zoo "1.0.0-beta3"]
                 [org.datavec/datavec-api "1.0.0-beta3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [me.raynes/fs "1.4.4"]
                 [compojure "1.6.1"]
                 [image-resizer "0.1.10"]
                 [http-kit "2.3.0"]]
  :aliases {"gather" ["run" "-m" "blitzcheat-ml.core" "gather"]
            "play" ["run" "-m" "blitzcheat-ml.core" "play"]
            "prep" ["run" "-m" "blitzcheat-ml.prep"]
            "classify" ["run" "-m" "blitzcheat-ml.classify"]
            "emnist" ["run" "-m" "emnist.core"]}
  :main ^:skip-aot blitzcheat-ml.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :jvm-opts ["-Xmx16G" "-Dorg.bytedeco.javacpp.maxbytes=20G" "-Dorg.bytedeco.javacpp.maxphysicalbytes=38G"])
