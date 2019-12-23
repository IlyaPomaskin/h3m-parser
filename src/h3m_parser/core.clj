(ns h3m-parser.core
  (:require [clojure.java.io :as io]
            [org.clojars.smee.binary.core :as binary]
            [h3m-parser.h3m :as h3m]
            [h3m-parser.lod :as lod]
            [h3m-parser.def :as def]))


(defn parse-h3m [file-path]
  (some-> file-path
          (io/input-stream)
          (java.util.zip.GZIPInputStream. 1)
          (io/input-stream)
          (as-> stream (binary/decode h3m/root stream))))


(defn parse-lod [file-path]
  (some-> file-path
          (io/input-stream)
          (as-> stream (binary/decode lod/root stream))))


(defn parse-def [file-path]
  (some-> file-path
          (io/input-stream)
          (as-> stream (binary/decode def/root stream))))
