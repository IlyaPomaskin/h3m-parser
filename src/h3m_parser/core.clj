(ns h3m-parser.core
  (:require [clojure.java.io :as io]
            [org.clojars.smee.binary.core :as binary]
            [h3m-parser.h3m :as h3m]
            [h3m-parser.lod :as lod]
            [h3m-parser.def :as def]
            [h3m-parser.frame :as frame])
  (:import java.io.RandomAccessFile))


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


(defn parse-def-frame
  ([^String file-path group-index file-index]
   (parse-def-frame file-path (parse-def file-path) group-index file-index))
  ([^String file-path def-info group-index file-index]
   (let [name (get-in def-info [:groups group-index :names file-index])
         offsets (get-in def-info [:groups group-index :offsets])
         offset (get offsets file-index)
         raf (new RandomAccessFile file-path "r")
         legacy-def? (def/legacy? raf offsets)]
     (when (and (some? name) (some? offset))
       (frame/parse raf offset legacy-def?)))))