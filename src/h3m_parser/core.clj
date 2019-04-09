(ns h3m-parser.core
  (:require [clojure.spec.alpha :as s]
            [org.clojars.smee.binary.core :as b]
            [clojure.pprint :as pp]
            [h3m-parser.objects :as h3m-objects]
            [h3m-parser.h3m :as h3m]
            [clojure.java.io :as io])
  (:import org.clojars.smee.binary.core.BinaryIO))


(defn stream->bytes [is]
  (loop [b (.read is) accum []]
    (if (< b 0)
      accum
      (recur (.read is) (conj accum b)))))


(defn stream->byte-array [is]
  (let [baos (java.io.ByteArrayOutputStream.)]
    (io/copy is baos)
    (.toByteArray baos)))


(defn read-h3m [filename]
  (with-open [in (new java.util.zip.GZIPInputStream (io/input-stream filename) 1)]
    (stream->bytes in)))


(defn -main [file-path]
  (when (not (.exists (io/file file-path)))
    (println "File" file-path "not exists"))
  (some-> file-path
          (as-> path (when (.exists (io/file path))
                       path))
          (io/input-stream)
          (java.util.zip.GZIPInputStream. 1)
          (io/input-stream)
          (as-> stream (b/decode h3m/root stream))
          (dissoc :players)
          (dissoc :predefined-heroes)
          (dissoc :placeholder-1)
          (dissoc :placeholder-2)
          (dissoc :placeholder-3)
          (dissoc :terrain)
          (dissoc :defs)
          (pp/pprint)))



