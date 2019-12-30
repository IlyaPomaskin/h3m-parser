(ns h3m-parser.core
  (:require
   [org.clojars.smee.binary.core :as binary]
   [h3m-parser.h3m :as h3m]
   [h3m-parser.lod :as lod]
   [h3m-parser.def :as def-file])
  (:import
   [java.io FileInputStream]
   [java.util.zip Inflater InflaterInputStream GZIPInputStream]))


(defn parse-h3m [in]
  (binary/decode h3m/root (new GZIPInputStream in)))


(defn parse-lod [in]
  (binary/decode lod/root in))


(defn parse-def [in]
  (binary/decode def-file/root in))


(defn get-def-stream-from-lod
  [lod-def-info ^FileInputStream in]
  (let [{compressed-size :compressed-size
         offset :offset} lod-def-info]
    (.position (.getChannel in) (long offset))
    (if (pos? compressed-size)
      (new InflaterInputStream in (new Inflater) compressed-size)
      in)))
