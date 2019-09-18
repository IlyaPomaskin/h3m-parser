(ns h3m-parser.core
  (:require [clojure.java.io :as io]
            [org.clojars.smee.binary.core :as binary]
            [h3m-parser.h3m :as h3m]))


(defn parse-stream [stream]
  (binary/decode h3m/root stream))


(defn parse-file [file-path]
  (some-> file-path
          (io/input-stream)
          (java.util.zip.GZIPInputStream. 1)
          (io/input-stream)
          (parse-stream)))